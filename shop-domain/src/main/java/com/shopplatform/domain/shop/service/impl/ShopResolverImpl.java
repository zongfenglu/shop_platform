package com.shopplatform.domain.shop.service.impl;

import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.mp.MpAuthorizerService;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.security.ClientTenantFilter;
import org.springframework.stereotype.Component;

/**
 * {@link ClientTenantFilter.ShopResolver} 的落地实现，供消费者端(shop-client-api)注册过滤器时使用。
 * 放在 shop-domain 而不是 shop-client-api，是因为反查逻辑本质是对 shop_domain 表的查询，
 * 属于领域层能力，client 模块只负责把它接到过滤器上（见 ClientFilterConfig）。
 */
@Component
public class ShopResolverImpl implements ClientTenantFilter.ShopResolver {

    private final ShopDomainService shopDomainService;
    private final MpAuthorizerService mpAuthorizerService;
    private final ShopService shopService;

    public ShopResolverImpl(ShopDomainService shopDomainService,
                            MpAuthorizerService mpAuthorizerService,
                            ShopService shopService) {
        this.shopDomainService = shopDomainService;
        this.mpAuthorizerService = mpAuthorizerService;
        this.shopService = shopService;
    }

    @Override
    public Long resolveByHost(String host) {
        String normalized = normalizeHost(host);
        if (normalized.isEmpty()) {
            return null;
        }
        ShopDomain domain = shopDomainService.findByDomain(normalized);
        if (domain != null) {
            return domain.getShopId();
        }
        if (normalized.startsWith("www.")) {
            domain = shopDomainService.findByDomain(normalized.substring(4));
            return domain == null ? null : domain.getShopId();
        }
        return null;
    }

    @Override
    public Long resolveByAppId(String appId) {
        return mpAuthorizerService.findShopIdByAppId(appId);
    }

    @Override
    public void ensureAccessible(Long shopId) {
        Shop shop;
        try {
            shop = shopService.getByIdWithTenant(shopId);
        } catch (TenantAccessDeniedException e) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "无法识别请求所属商城");
        }
        String status = shop.getStatus();
        if (ShopStatus.DISABLED.getCode().equals(status) || ShopStatus.ARCHIVED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.TENANT_DISABLED);
        }
    }

    static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String s = host.trim().toLowerCase();
        int colon = s.indexOf(':');
        if (colon >= 0) {
            s = s.substring(0, colon);
        }
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
