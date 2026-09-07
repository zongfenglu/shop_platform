package com.shopplatform.framework.security;

import com.shopplatform.framework.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 商户后台（/store/**）鉴权过滤器：从 JWT 中解出 shopId + storeUserId，
 * 写入 TenantContext / LoginUserContext，请求结束后强制清理。
 * <p>
 * 这里的 try/finally 是整套多租户系统里最不能出错的一段代码：
 * 一旦某次请求异常导致 clear() 没被调用，线程池下一次复用这个线程处理别的租户的请求时，
 * ThreadLocal 里残留的 shopId 会直接造成跨租户数据泄露。
 * 见文档三 §2.1、§2.2。
 */
public class StoreTenantFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public StoreTenantFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null) {
                try {
                    Claims claims = jwtTokenProvider.parse(token);
                    Long shopId = claims.get("shopId", Long.class);
                    Long storeUserId = Long.valueOf(claims.getSubject());
                    boolean impersonation = Boolean.TRUE.equals(claims.get("platformImpersonation", Boolean.class));

                    if (shopId != null) {
                        TenantContext.set(shopId);
                    }
                    LoginUserContext.set(new LoginUserContext.LoginUser(
                            storeUserId, shopId, claims.get("username", String.class), impersonation));
                } catch (JwtException | IllegalArgumentException e) {
                    // token 非法/过期：不在 Filter 里直接 401，交给后续鉴权注解/拦截器统一处理，
                    // 这里只保证不把上下文设置成脏数据。
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            LoginUserContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
