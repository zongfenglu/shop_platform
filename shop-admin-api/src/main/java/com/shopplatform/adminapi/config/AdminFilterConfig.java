package com.shopplatform.adminapi.config;

import com.shopplatform.framework.security.AdminAuthFilter;
import com.shopplatform.framework.security.JwtTokenProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册平台超管鉴权过滤器。
 * <p>
 * 与商户端不同，这个过滤器是**强制拦断**的：没有合法 token 的 /admin/** 请求直接 401，
 * 不能像 StoreTenantFilter 那样"放过去让后续逻辑兜底" —— 平台接口没有租户上下文可兜底，
 * 放过去就是无鉴权访问全平台数据。登录接口在过滤器内部白名单里放开。
 */
@Configuration
public class AdminFilterConfig {

    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilter(JwtTokenProvider jwtTokenProvider) {
        FilterRegistrationBean<AdminAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdminAuthFilter(jwtTokenProvider));
        registration.addUrlPatterns("/admin/*");
        registration.setOrder(1);
        return registration;
    }
}
