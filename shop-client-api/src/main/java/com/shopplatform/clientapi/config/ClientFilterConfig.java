package com.shopplatform.clientapi.config;

import com.shopplatform.framework.security.ClientTenantFilter;
import com.shopplatform.framework.security.JwtTokenProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册消费者端(/api/**)租户识别过滤器。ShopResolver 的落地实现见 shop-domain 的 ShopResolverImpl，
 * 这里只负责组装、注册 URL 匹配范围，避免过滤器逻辑与反查逻辑耦合在同一个类里（见 ClientTenantFilter 类注释）。
 */
@Configuration
public class ClientFilterConfig {

    @Bean
    public FilterRegistrationBean<ClientTenantFilter> clientTenantFilter(
            JwtTokenProvider jwtTokenProvider, ClientTenantFilter.ShopResolver shopResolver) {
        FilterRegistrationBean<ClientTenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ClientTenantFilter(jwtTokenProvider, shopResolver));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
