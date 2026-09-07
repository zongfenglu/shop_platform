package com.shopplatform.framework.security;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
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
 * 消费者端（/api/**）租户识别过滤器。见文档三 §2.2 租户识别来源：
 * ① 请求头 X-Shop-Id（小程序由 ext.json 注入，最高优先级，最明确）
 * ② Host 反查域名表（H5 走自定义域名/泛域名场景）
 * ③ 小程序 AppID 反查（无法拿到 shopId 时的兜底）
 * <p>
 * 域名/AppID 反查 shopId 依赖 shop-domain 模块的查询能力，这里只定义过滤器骨架与优先级，
 * 具体反查逻辑通过 {@link ShopResolver} 接口交给上层模块实现，避免 framework 反向依赖 domain。
 */
public class ClientTenantFilter extends OncePerRequestFilter {

    private static final String HEADER_SHOP_ID = "X-Shop-Id";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ShopResolver shopResolver;

    public ClientTenantFilter(JwtTokenProvider jwtTokenProvider, ShopResolver shopResolver) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.shopResolver = shopResolver;
    }

    /**
     * 支付渠道回调（如微信支付 notify）不经过这里：回调请求来自支付渠道服务器，
     * 既没有我们的 X-Shop-Id/X-Mini-AppId 头，Host 也是我们自己的公网域名而不是租户子域名，
     * 三条识别来源全部落空。这类接口的 shopId 直接编码在 URL 路径里，由对应 Controller 自己
     * TenantContext.set()/clear()，不需要也不应该套用这里的通用识别逻辑。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/pay/notify/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Long shopId = resolveShopId(request);
            if (shopId == null) {
                throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "无法识别请求所属商城");
            }
            shopResolver.ensureAccessible(shopId);
            TenantContext.set(shopId);
            resolveLoginUser(request, shopId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            LoginUserContext.clear();
        }
    }

    private Long resolveShopId(HttpServletRequest request) {
        String headerShopId = request.getHeader(HEADER_SHOP_ID);
        if (headerShopId != null && !headerShopId.isBlank()) {
            try {
                return Long.valueOf(headerShopId);
            } catch (NumberFormatException ignored) {
                // 非法值走后续兜底
            }
        }
        String host = request.getServerName();
        Long byHost = shopResolver.resolveByHost(host);
        if (byHost != null) {
            return byHost;
        }
        String appId = request.getHeader("X-Mini-AppId");
        if (appId != null && !appId.isBlank()) {
            return shopResolver.resolveByAppId(appId);
        }
        return null;
    }

    /** 游客也可浏览，登录态是可选的——未带 token 或 token 非法都不阻断请求，只是不设置 LoginUserContext。 */
    private void resolveLoginUser(HttpServletRequest request, Long shopId) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parse(header.substring(BEARER_PREFIX.length()));
            Long userId = Long.valueOf(claims.getSubject());
            LoginUserContext.set(new LoginUserContext.LoginUser(userId, shopId, null, false));
        } catch (JwtException | IllegalArgumentException ignored) {
            // token 非法：按游客处理，不阻断浏览
        }
    }

    /** 由 shop-domain 模块实现，避免 framework 反向依赖 domain。 */
    public interface ShopResolver {
        Long resolveByHost(String host);

        Long resolveByAppId(String appId);

        /** 停用/归档商城拒绝用户端访问。默认放行，由落地实现按 shop.status 拦截。 */
        default void ensureAccessible(Long shopId) {
        }
    }
}
