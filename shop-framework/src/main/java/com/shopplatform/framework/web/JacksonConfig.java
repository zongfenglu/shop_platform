package com.shopplatform.framework.web;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全平台所有主键/关联ID都是雪花算法生成的 64 位 long（见各 BaseEntity），常规值有 19 位十进制数字，
 * 远超 JavaScript {@code Number.MAX_SAFE_INTEGER}（2^53-1，约 16 位）。Long 按 JSON 数字序列化时，
 * 前端 JSON.parse 会把超出安全整数范围的值静默舍入到最近的可表示浮点数——不报错、不是 null，
 * 就是一个悄悄变了的、看起来完全正常的数字。用这个被舍入过的 id 去请求详情/更新接口，
 * 后端查不到对应记录，返回的是"无权限访问"或"资源不存在"，现象和真正的越权/数据不存在一模一样，
 * 排查起来极容易被带偏方向。
 * <p>
 * 标准解法（Twitter/Discord 等所有雪花ID系统的通行做法）：Long 一律按 JSON 字符串序列化，
 * 前端拿到的是 "2084943064879411202" 这样的字符串，原样透传给下一个请求，不经过数字运算，
 * 自然不会有精度问题。前端不需要为此做任何特殊处理——所有 id 字段本来就只用于展示和回传，
 * 从不参与数值运算。
 * <p>
 * 注册在 shop-framework 而不是各个 xxx-api 模块：四个 Spring Boot 应用都用
 * {@code @SpringBootApplication(scanBasePackages = "com.shopplatform")} 扫描，
 * 这一份配置能被全部应用自动拾取，不需要在每个模块里重复配置一遍。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsStringJacksonCustomizer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return builder -> builder.modulesToInstall(modules -> modules.add(module));
    }
}
