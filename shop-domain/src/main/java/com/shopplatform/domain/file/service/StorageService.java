package com.shopplatform.domain.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象。当前只有本地磁盘实现（{@code LocalStorageServiceImpl}），
 * 后续接阿里云 OSS / 腾讯云 COS 时只需新增一个实现类并切换 Bean，调用方无需改动。
 */
public interface StorageService {

    /**
     * 保存一张图片，返回可直接用于 {@code <img src>} 的相对 URL。
     *
     * @param file   上传的文件
     * @param shopId 商户ID，仅用于目录分组（不是访问控制，见 UploadResourceConfig 的安全说明）
     */
    StoredFile storeImage(MultipartFile file, Long shopId);

    /**
     * 保存一个视频（装修视频组等场景），返回可用于 {@code <video src>} 的相对 URL。
     * 与 storeImage 相同的安全约束：magic byte 嗅探、服务端生成文件名。
     */
    StoredFile storeVideo(MultipartFile file, Long shopId);

    /**
     * 统计某商城本地上传目录占用。目录不存在时返回 0，不抛错。
     * shopId 必须是 Long（服务端已有的租户 ID），禁止传入客户端路径片段。
     */
    long directorySize(Long shopId);

    /**
     * @param url  相对 URL，形如 {@code /uploads/{shopId}/{yyyyMM}/{uuid}.jpg}
     * @param name 原始文件名，仅用于素材库展示（已做净化，不参与任何路径拼接）
     * @param size 字节数
     */
    record StoredFile(String url, String name, long size) {
    }
}
