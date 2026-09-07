package com.shopplatform.domain.shop.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.service.DiyPageService;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.shop.entity.PackageTpl;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.service.PackageTplService;
import com.shopplatform.domain.shop.service.ShopOrderService;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.shop.service.StoreUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopSubscriptionServiceImplTest {

    private ShopService shopService;
    private ShopPackageService shopPackageService;
    private PackageTplService packageTplService;
    private ShopOrderService shopOrderService;
    private ShopSubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        shopPackageService = mock(ShopPackageService.class);
        packageTplService = mock(PackageTplService.class);
        shopOrderService = mock(ShopOrderService.class);
        service = new ShopSubscriptionServiceImpl(
                shopService, shopPackageService, packageTplService, shopOrderService,
                mock(GoodsService.class), mock(StoreUserService.class),
                mock(DiyPageService.class), mock(OfflineStoreService.class),
                new ObjectMapper());
    }

    @Test
    void placeOrder_renew_writesPendingOfflineOrder() {
        Shop shop = shop(1001L, 14001L);
        ShopPackage pkg = new ShopPackage();
        pkg.setId(14001L);
        pkg.setPackageTplId(2L);
        PackageTpl tpl = tpl(2L, "{\"month\":299,\"year\":2680}");
        when(shopService.getOne(any())).thenReturn(shop);
        when(shopOrderService.count(any())).thenReturn(0L);
        when(shopPackageService.getOne(any())).thenReturn(pkg);
        when(packageTplService.getOne(any())).thenReturn(tpl);

        ShopOrder order = service.placeOrder(1001L, "renew", null, 12);
        assertEquals("renew", order.getType());
        assertEquals("pending", order.getPayStatus());
        assertEquals("offline", order.getPayMethod());
        assertEquals(new BigDecimal("2680.00"), order.getAmount());
        verify(shopOrderService).save(any(ShopOrder.class));
    }

    @Test
    void placeOrder_blocksSecondPending() {
        when(shopService.getOne(any())).thenReturn(shop(1001L, 14001L));
        when(shopOrderService.count(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.placeOrder(1001L, "renew", 2L, 12));
        assertEquals(ErrorCode.SHOP_ORDER_PENDING_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void confirmPaid_extendsExpireTime() {
        Shop shop = shop(1001L, 14001L);
        shop.setExpireTime(LocalDateTime.now().plusDays(10));
        ShopOrder order = new ShopOrder();
        order.setId(9L);
        order.setShopId(1001L);
        order.setType("renew");
        order.setPackageTplId(2L);
        order.setDurationMonth(12);
        order.setPayStatus("pending");
        ShopPackage pkg = new ShopPackage();
        pkg.setId(14001L);
        pkg.setShopId(1001L);
        pkg.setExpireTime(shop.getExpireTime());
        PackageTpl tpl = tpl(2L, "{\"year\":2680}");
        when(shopOrderService.getOne(any())).thenReturn(order);
        when(shopService.getOne(any())).thenReturn(shop);
        when(packageTplService.getOne(any())).thenReturn(tpl);
        when(shopPackageService.getOne(any())).thenReturn(pkg);

        ShopOrder paid = service.confirmPaid(9L);
        assertEquals("paid", paid.getPayStatus());
        verify(shopService).updateById(any(Shop.class));
        verify(shopPackageService).updateById(any(ShopPackage.class));
    }

    private static Shop shop(Long id, Long packageId) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setPackageId(packageId);
        shop.setStatus("normal");
        return shop;
    }

    private static PackageTpl tpl(Long id, String price) {
        PackageTpl tpl = new PackageTpl();
        tpl.setId(id);
        tpl.setName("标准版");
        tpl.setPrice(price);
        tpl.setMenus("[]");
        tpl.setQuota("{}");
        return tpl;
    }
}
