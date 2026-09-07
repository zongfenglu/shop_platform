package com.shopplatform.storeapi.config;

import com.shopplatform.framework.security.JwtTokenProvider;
import com.shopplatform.framework.security.StoreTenantFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册商户后台的租户识别过滤器。
 * <p>
 * /store/auth/login 本身不需要 TenantContext（登录时还不知道 shopId），
 * 但该请求没有 Authorization 头，过滤器内部逻辑本身已经处理了"无token则跳过设置"的情况，
 * 不会误伤登录接口——过滤器统一注册在 /store/* 上更简单，不需要为登录接口单独排除。
 */
@Configuration
public class StoreFilterConfig {

    @Bean
    public FilterRegistrationBean<StoreTenantFilter> storeTenantFilter(JwtTokenProvider jwtTokenProvider) {
        FilterRegistrationBean<StoreTenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new StoreTenantFilter(jwtTokenProvider));
        registration.addUrlPatterns("/store/*");
        registration.setOrder(1);
        return registration;
    }
}
