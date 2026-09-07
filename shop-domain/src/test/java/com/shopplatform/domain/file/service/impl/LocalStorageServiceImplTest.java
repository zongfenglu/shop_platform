package com.shopplatform.domain.file.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.file.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LocalStorageServiceImpl} 单测。这个类是对外的文件上传入口，
 * 下面每个用例对应一条真实的攻击路径，删任何一个都等于把洞放回去。
 * 用真实临时目录而不是 mock 文件系统——落盘路径本身就是被测对象。
 */
class LocalStorageServiceImplTest {

    /** JPEG magic：FF D8 FF */
    private static final byte[] JPEG_HEAD = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0 };
    /** PNG magic：89 50 4E 47 0D 0A 1A 0A */
    private static final byte[] PNG_HEAD = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };

    private LocalStorageServiceImpl service(Path dir) {
        return new LocalStorageServiceImpl(dir.toString(), "/uploads");
    }

    @Test
    void storeImage_jpeg_writesFileAndReturnsRelativeUrl(@TempDir Path dir) throws IOException {
        StorageService.StoredFile stored =
                service(dir).storeImage(new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_HEAD), 42L);

        assertTrue(stored.url().startsWith("/uploads/42/"), "URL 应带上 publicPrefix 与 shopId 目录");
        assertTrue(stored.url().endsWith(".jpg"));
        assertEquals(JPEG_HEAD.length, stored.size());

        // URL 与磁盘落点必须一致，否则前端拿到的地址是 404
        Path onDisk = dir.resolve(stored.url().replace("/uploads/", ""));
        assertTrue(Files.exists(onDisk), "文件应真实落盘在 URL 对应的位置");
    }

    /**
     * 类型必须由文件头决定，而不是 Content-Type：请求头完全由客户端控制。
     * 这里传一段 HTML 却声明成 image/png —— 如果实现信了请求头，就会把一个可执行/可渲染的
     * 文件以 .png 落进公开目录，配合某些浏览器的嗅探行为可以变成存储型 XSS。
     */
    @Test
    void storeImage_rejectsNonImageWithSpoofedContentType(@TempDir Path dir) {
        MockMultipartFile fake = new MockMultipartFile(
                "file", "evil.png", "image/png", "<html><script>alert(1)</script></html>".getBytes());

        BusinessException e = assertThrows(BusinessException.class, () -> service(dir).storeImage(fake, 42L));
        assertEquals(ErrorCode.UPLOAD_FILE_TYPE_NOT_ALLOWED.getCode(), e.getCode());
    }

    /**
     * 落盘文件名必须是服务端生成的 UUID，不能包含客户端原始文件名的任何片段。
     * 传入带 ../ 的文件名：如果实现拿原名拼路径，文件会跑到 root 之外。
     */
    @Test
    void storeImage_ignoresClientFilename_noPathTraversal(@TempDir Path dir) throws IOException {
        StorageService.StoredFile stored = service(dir).storeImage(
                new MockMultipartFile("file", "../../../../evil.jpg", "image/jpeg", JPEG_HEAD), 42L);

        assertFalse(stored.url().contains(".."), "URL 不能出现 ..");
        assertFalse(stored.url().contains("evil"), "落盘名不能包含客户端传来的原名");

        // 整个临时目录里只应有这一个文件，且必须在 root 之下
        try (var walk = Files.walk(dir)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            assertEquals(1, files.size());
            assertTrue(files.get(0).toAbsolutePath().normalize().startsWith(dir.toAbsolutePath().normalize()),
                    "文件必须落在存储根目录内");
        }
    }

    /** 原名仅用于素材库展示，落库前要净化掉路径分隔符和可能触发 XSS 的字符。 */
    @Test
    void storeImage_sanitizesDisplayName(@TempDir Path dir) {
        StorageService.StoredFile stored = service(dir).storeImage(
                new MockMultipartFile("file", "../a/<img onerror=x>.png", "image/png", PNG_HEAD), 7L);

        assertFalse(stored.name().contains("/"));
        assertFalse(stored.name().contains("\\"));
        assertFalse(stored.name().contains(".."));
        assertFalse(stored.name().contains("<"));
        assertFalse(stored.name().contains(">"));
    }

    @Test
    void storeImage_rejectsEmptyFile(@TempDir Path dir) {
        BusinessException e = assertThrows(BusinessException.class, () ->
                service(dir).storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]), 1L));
        assertEquals(ErrorCode.UPLOAD_FILE_EMPTY.getCode(), e.getCode());
    }

    @Test
    void storeImage_rejectsNull(@TempDir Path dir) {
        BusinessException e = assertThrows(BusinessException.class, () -> service(dir).storeImage(null, 1L));
        assertEquals(ErrorCode.UPLOAD_FILE_EMPTY.getCode(), e.getCode());
    }

    /** 超过 5MB 直接拒绝：Spring multipart 限制是第一道闸，这里兜底防配置漏改。 */
    @Test
    void storeImage_rejectsOversizedFile(@TempDir Path dir) {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(JPEG_HEAD, 0, big, 0, JPEG_HEAD.length);

        BusinessException e = assertThrows(BusinessException.class, () ->
                service(dir).storeImage(new MockMultipartFile("file", "big.jpg", "image/jpeg", big), 1L));
        assertEquals(ErrorCode.UPLOAD_FILE_TOO_LARGE.getCode(), e.getCode());
    }

    /** 扩展名取自嗅探结果，不取自客户端文件名：内容是 PNG 就必须存成 .png。 */
    @Test
    void storeImage_extensionComesFromContentNotFilename(@TempDir Path dir) {
        StorageService.StoredFile stored = service(dir).storeImage(
                new MockMultipartFile("file", "actually-a-png.jpg", "image/jpeg", PNG_HEAD), 1L);

        assertTrue(stored.url().endsWith(".png"), "内容是 PNG，落盘扩展名应为 .png 而非文件名里的 .jpg");
    }

    /** 两次上传同一张图不能互相覆盖（UUID 命名的意义之一）。 */
    @Test
    void storeImage_twoUploadsOfSameContentGetDistinctUrls(@TempDir Path dir) {
        LocalStorageServiceImpl svc = service(dir);
        String first = svc.storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_HEAD), 1L).url();
        String second = svc.storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_HEAD), 1L).url();

        assertNotEquals(first, second);
    }

    @Test
    void directorySize_sumsShopFolderAndIgnoresMissing(@TempDir Path dir) throws IOException {
        LocalStorageServiceImpl svc = service(dir);
        assertEquals(0L, svc.directorySize(99L));
        svc.storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_HEAD), 7L);
        assertEquals(JPEG_HEAD.length, svc.directorySize(7L));
        assertEquals(0L, svc.directorySize(null));
    }
}
