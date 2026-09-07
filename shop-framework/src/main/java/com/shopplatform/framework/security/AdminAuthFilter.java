package com.shopplatform.framework.security;

import com.shopplatform.common.result.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 平台超管（/admin/**）鉴权过滤器。
 * <p>
 * 与 {@link StoreTenantFilter} / {@link ClientTenantFilter} 的关键差异：那两个是"识别"过滤器，
 * token 缺失或非法时只是不设置上下文、把请求放过去（由后续 TenantContext.getRequired() 兜底报错）；
 * 平台超管接口没有租户上下文可依赖，**放过去就等于无鉴权访问全平台数据**，
 * 所以这里必须直接拦断返回 401。
 * <p>
 * 平台管理员 JWT 只含 sub=userId，不带 shopId（见 AdminAuthController 与文档三 §2.2），
 * 因此本过滤器只写 LoginUserContext，不碰 TenantContext。
 */
public class AdminAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 免鉴权路径。登录接口本身当然要放开；actuator 不在 /admin/** 下，不需要在这里排除。
     */
    private static final Set<String> WHITELIST = Set.of("/admin/auth/login");

    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (WHITELIST.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token == null) {
            reject(response, "未登录或登录已过期");
            return;
        }

        Long userId;
        String username;
        try {
            Claims claims = jwtTokenProvider.parse(token);
            userId = Long.valueOf(claims.getSubject());
            username = claims.get("username", String.class);
            // 商户后台的 token 带 shopId，平台 token 不带。拿商户 token 调平台接口必须拒绝，
            // 否则任意商户员工都能越权访问全平台数据。
            if (claims.get("shopId") != null) {
                reject(response, "无权限访问");
                return;
            }
        } catch (JwtException | IllegalArgumentException e) {
            reject(response, "未登录或登录已过期");
            return;
        }

        try {
            LoginUserContext.set(new LoginUserContext.LoginUser(userId, null, username, false));
            filterChain.doFilter(request, response);
        } finally {
            // 必须清理：线程池复用线程时，残留的 LoginUser 会让下一个请求带着上一个人的身份执行。
            LoginUserContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    /**
     * 直接写 Result 结构的 JSON，与全局异常处理器的响应体保持一致，
     * 前端拦截器只需要认 code 一种格式。
     */
    private void reject(HttpServletResponse response, String msg) throws IOException {
        // HTTP 层用 401，业务码沿用 ErrorCode，前端靠 401 触发跳登录页
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        int code = "无权限访问".equals(msg) ? ErrorCode.FORBIDDEN.getCode() : ErrorCode.UNAUTHORIZED.getCode();
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\"}");
    }
}
