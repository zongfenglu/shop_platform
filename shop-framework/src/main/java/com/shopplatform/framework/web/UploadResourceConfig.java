package com.shopplatform.framework.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 把本地上传目录挂成静态资源，商户上传的图片可以直接通过 {@code /uploads/**} 访问。
 * <p>
 * 放在 shop-framework 而不是某个 api 模块：四个 Spring Boot 应用都用
 * {@code scanBasePackages = "com.shopplatform"}，因此 store-api（商户上传）和
 * client-api（消费者读取）会各自加载这份配置，指向 docker compose 里同一个共享卷。
 * <p>
 * <b>安全边界（重要）</b>：{@code /uploads/**} 是<b>有意公开</b>的，不做任何鉴权。
 * 装修页里的轮播图、商品图必须能被<b>未登录</b>的 C 端用户加载，这是它存在的意义。
 * URL 路径里的 shopId 段只是为了目录分组和便于运维排查，<b>不构成访问控制</b>——
 * 任何人拿到 URL 都能读。因此这里只能放商户主动公开展示的素材，
 * 绝不能用同一套机制存放营业执照、身份证、对账单等需要鉴权的私有文件；
 * 那类文件后续要单独走带 token 校验的下载接口，不要图省事复用这个目录。
 */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    private final String localDir;

    public UploadResourceConfig(@Value("${shop.storage.local-dir:./data/uploads}") String localDir) {
        this.localDir = localDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // toUri() 保证结尾带 "/" 且是合法的 file: URL（Windows 的反斜杠、盘符都会被正确处理）
        Path root = Paths.get(localDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(root.toUri().toString())
                .setCachePeriod(86400);
    }
}
