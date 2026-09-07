package com.shopplatform.domain.shop.service.impl;

import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.mp.MpAuthorizerService;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.shop.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShopResolverImplTest {

    private ShopDomainService shopDomainService;
    private ShopService shopService;
    private ShopResolverImpl resolver;

    @BeforeEach
    void setUp() {
        shopDomainService = mock(ShopDomainService.class);
        shopService = mock(ShopService.class);
        resolver = new ShopResolverImpl(shopDomainService, mock(MpAuthorizerService.class), shopService, "shop.com");
    }

    @Test
    void resolveByHost_usesVerifiedBindingAndStripsPort() {
        ShopDomain row = new ShopDomain();
        row.setShopId(1001L);
        when(shopDomainService.findByDomain("demo.localhost")).thenReturn(row);
        assertEquals(1001L, resolver.resolveByHost("Demo.Localhost:5175"));
    }

    @Test
    void resolveByHost_fallsBackWithoutWww() {
        ShopDomain row = new ShopDomain();
        row.setShopId(1001L);
        when(shopDomainService.findByDomain("www.example.com")).thenReturn(null);
        when(shopDomainService.findByDomain("example.com")).thenReturn(row);
        assertEquals(1001L, resolver.resolveByHost("www.example.com"));
    }

    @Test
    void resolveByAppId_delegatesToAuthorizer() {
        var mp = mock(MpAuthorizerService.class);
        when(mp.findShopIdByAppId("wx123")).thenReturn(1001L);
        resolver = new ShopResolverImpl(shopDomainService, mp, shopService, "shop.com");
        assertEquals(1001L, resolver.resolveByAppId("wx123"));
    }

    @Test
    void resolveByHost_unknown_returnsNull() {
        when(shopDomainService.findByDomain("unknown.shop.com")).thenReturn(null);
        assertNull(resolver.resolveByHost("unknown.shop.com"));
        assertNull(resolver.resolveByHost(" "));
    }

    @Test
    void ensureAccessible_disabled_throws() {
        Shop shop = new Shop();
        shop.setId(1001L);
        shop.setStatus(ShopStatus.DISABLED.getCode());
        when(shopService.getByIdWithTenant(1001L)).thenReturn(shop);
        assertThrows(BusinessException.class, () -> resolver.ensureAccessible(1001L));
    }

    @Test
    void resolveByHost_fallsBackToShopCodeOnPlatformDomain() {
        Shop shop = new Shop();
        shop.setId(1001L);
        when(shopDomainService.findByDomain("demo.shop.test")).thenReturn(null);
        when(shopService.findByCode("demo")).thenReturn(shop);
        resolver = new ShopResolverImpl(shopDomainService, mock(MpAuthorizerService.class), shopService, "shop.test");
        assertEquals(1001L, resolver.resolveByHost("Demo.Shop.Test"));
    }

    @Test
    void resolveByHost_reservedSubdomain_notAShop() {
        resolver = new ShopResolverImpl(shopDomainService, mock(MpAuthorizerService.class), shopService, "shop.test");
        assertNull(resolver.resolveByHost("admin.shop.test"));
        assertNull(resolver.resolveByHost("store.shop.test"));
    }
}
