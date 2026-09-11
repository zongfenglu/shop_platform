package com.shopplatform.domain.file.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.file.service.ObjectStorageUploader;
import com.shopplatform.domain.file.service.StorageService;
import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import com.shopplatform.domain.setting.service.StoreOperationSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商城文件存储入口。本地模式写磁盘；对象存储模式在完成同样的安全校验后交给对应厂商适配器。
 * docker 本地模式下挂共享卷 {@code uploads-data:/data/uploads}，store-api 写、client-api 读。
 * <p>
 * <b>这个类里的三处防御都不是可选的</b>，上传接口是典型的攻击入口：
 * <ol>
 *   <li><b>文件名一律服务端生成 UUID</b>，绝不使用 {@code getOriginalFilename()} 参与路径拼接。
 *       客户端可以把文件名传成 {@code ../../../etc/passwd} 或 {@code ..\\..\\web.config}，
 *       任何"取原名再拼目录"的写法都是路径穿越漏洞。原名只在净化后存进 DB 供素材库展示。</li>
 *   <li><b>类型由 magic byte 决定，不信 Content-Type</b>。请求头完全由客户端控制，
 *       把一个 .jsp/.html 声明成 {@code image/jpeg} 是一行 curl 的事。只读文件头前 12 字节判断，
 *       并且用嗅探出的类型来决定落盘扩展名——扩展名也不取自客户端。</li>
 *   <li><b>大小上限</b>。Spring 的 multipart 限制是第一道，这里再兜一道，
 *       避免配置漏改时磁盘被打满。</li>
 * </ol>
 */
@Service
public class LocalStorageServiceImpl implements StorageService {

    /** 5MB。与 application.yml 的 spring.servlet.multipart.max-file-size 保持一致。 */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    /** 视频上限 50MB。application.yml 的 multipart 上限须一并调到 ≥50MB，否则先被 Spring 拒掉。 */
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final Path root;
    private final String publicPrefix;
    private final StoreOperationSettingService settingService;
    private final Map<String, ObjectStorageUploader> objectStorageUploaders;

    @Autowired
    public LocalStorageServiceImpl(
            @Value("${shop.storage.local-dir:./data/uploads}") String localDir,
            @Value("${shop.storage.public-prefix:/uploads}") String publicPrefix,
            StoreOperationSettingService settingService,
            List<ObjectStorageUploader> objectStorageUploaders) {
        this.root = Paths.get(localDir).toAbsolutePath().normalize();
        this.publicPrefix = publicPrefix.endsWith("/") ? publicPrefix.substring(0, publicPrefix.length() - 1) : publicPrefix;
        this.settingService = settingService;
        this.objectStorageUploaders = objectStorageUploaders.stream()
                .collect(Collectors.toUnmodifiableMap(ObjectStorageUploader::provider, Function.identity()));
    }

    LocalStorageServiceImpl(String localDir, String publicPrefix) {
        this.root = Paths.get(localDir).toAbsolutePath().normalize();
        this.publicPrefix = publicPrefix.endsWith("/") ? publicPrefix.substring(0, publicPrefix.length() - 1) : publicPrefix;
        this.settingService = null;
        this.objectStorageUploaders = Collections.emptyMap();
    }

    LocalStorageServiceImpl(String localDir, String publicPrefix, StoreOperationSettingService settingService) {
        this(localDir, publicPrefix, settingService, List.of());
    }

    @Override
    public StoredFile storeImage(MultipartFile file, Long shopId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        StoreOperationSetting setting = currentSetting();
        long maxSize = megabytes(setting.getImageMaxMb(), MAX_SIZE);
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_TOO_LARGE);
        }

        String ext = sniffImageExtension(file);
        return store(file, shopId, ext, imageContentType(ext), setting, "图片");
    }

    @Override
    public StoredFile storeVideo(MultipartFile file, Long shopId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        StoreOperationSetting setting = currentSetting();
        long maxSize = megabytes(setting.getVideoMaxMb(), MAX_VIDEO_SIZE);
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_TOO_LARGE);
        }
        String ext = sniffVideoExtension(file);
        return store(file, shopId, ext, "video/mp4", setting, "视频");
    }

    private StoredFile store(MultipartFile file, Long shopId, String ext, String contentType,
                             StoreOperationSetting setting, String fileLabel) {
        String monthDir = LocalDate.now().format(MONTH);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String provider = setting.getUploadProvider();
        if (provider == null || provider.isBlank() || "local".equals(provider)) {
            Path dir = root.resolve(String.valueOf(shopId)).resolve(monthDir);
            try {
                Files.createDirectories(dir);
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, dir.resolve(filename));
                }
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, fileLabel + "保存失败");
            }
            String url = publicPrefix(setting) + "/" + shopId + "/" + monthDir + "/" + filename;
            return new StoredFile(url, safeDisplayName(file.getOriginalFilename(), ext), file.getSize());
        }

        ObjectStorageUploader uploader = objectStorageUploaders.get(provider);
        if (uploader == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的存储渠道: " + provider);
        }
        String objectKey = "shop/" + shopId + "/" + monthDir + "/" + filename;
        String url = uploader.upload(setting, objectKey, file, contentType);
        return new StoredFile(url, safeDisplayName(file.getOriginalFilename(), ext), file.getSize());
    }

    private String imageContentType(String ext) {
        return switch (ext) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    @Override
    public long directorySize(Long shopId) {
        if (shopId == null || shopId <= 0) {
            return 0L;
        }
        Path shopDir = root.resolve(String.valueOf(shopId)).normalize();
        if (!shopDir.startsWith(root) || !Files.isDirectory(shopDir)) {
            return 0L;
        }
        try (var walk = Files.walk(shopDir)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * 读文件头判断真实图片类型。返回落盘用的扩展名；不是支持的图片格式就直接拒绝。
     * 不看 {@code file.getContentType()}——那是客户端自己填的字符串。
     */
    private String sniffImageExtension(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        if (read < 3) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_TYPE_NOT_ALLOWED);
        }

        // JPEG: FF D8 FF
        if (u(head[0]) == 0xFF && u(head[1]) == 0xD8 && u(head[2]) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (read >= 8 && u(head[0]) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && u(head[4]) == 0x0D && u(head[5]) == 0x0A && u(head[6]) == 0x1A && u(head[7]) == 0x0A) {
            return "png";
        }
        // GIF: "GIF87a" / "GIF89a"
        if (read >= 6 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F') {
            return "gif";
        }
        // WEBP: "RIFF" .... "WEBP"
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        throw new BusinessException(ErrorCode.UPLOAD_FILE_TYPE_NOT_ALLOWED);
    }

    private static int u(byte b) {
        return b & 0xFF;
    }

    private long megabytes(Integer configured, long fallback) {
        return configured == null || configured <= 0 ? fallback : configured.longValue() * 1024 * 1024;
    }

    private StoreOperationSetting currentSetting() {
        if (settingService != null) return settingService.getOrCreate();
        StoreOperationSetting defaults = new StoreOperationSetting();
        defaults.setImageMaxMb(5);
        defaults.setVideoMaxMb(50);
        return defaults;
    }

    private String publicPrefix(StoreOperationSetting setting) {
        String domain = setting.getUploadDomain();
        return domain == null || domain.isBlank() ? publicPrefix : domain;
    }

    /**
     * 读文件头判断真实视频类型，与图片同样不信 Content-Type。只放行 MP4 家族
     * （ftyp box，涵盖 mp4/m4v/mov 的 isom/mp42/qt 等 brand）——H5 video 标签与小程序
     * video 组件的公共交集就是 MP4/H.264，webm/avi 在小程序端根本放不了，收了也是坏体验。
     */
    private String sniffVideoExtension(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        // MP4: 偏移 4 起为 "ftyp"
        if (read >= 8 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
            return "mp4";
        }
        throw new BusinessException(ErrorCode.UPLOAD_FILE_TYPE_NOT_ALLOWED, "仅支持 MP4 格式视频");
    }

    /**
     * 原始文件名只用于素材库列表展示，这里做净化后才允许落库：
     * 去掉所有路径分隔符（防止展示层某天把它当路径用）、限长、去掉可能触发 XSS 的尖括号引号。
     */
    private String safeDisplayName(String original, String ext) {
        if (original == null || original.isBlank()) {
            return "image." + ext;
        }
        String cleaned = original
                .replaceAll("[\\\\/]", "_")
                .replaceAll("[<>\"'`]", "")
                .replace("..", "_")
                .trim();
        if (cleaned.isBlank()) {
            return "image." + ext;
        }
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }
}
