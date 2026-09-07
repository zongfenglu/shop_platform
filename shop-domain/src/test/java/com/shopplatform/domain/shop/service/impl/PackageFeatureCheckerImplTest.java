package com.shopplatform.domain.shop.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PackageFeatureCheckerImplTest {

    private static final Long SHOP_ID = 1L;
    private static final Long PACKAGE_ID = 9L;

    private ShopService shopService;
    private ShopPackageService shopPackageService;
    private PackageFeatureCheckerImpl checker;

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        shopPackageService = mock(ShopPackageService.class);
        checker = new PackageFeatureCheckerImpl(shopService, shopPackageService, new ObjectMapper());
    }

    private Shop shopWithPackage() {
        Shop shop = new Shop();
        shop.setId(SHOP_ID);
        shop.setPackageId(PACKAGE_ID);
        return shop;
    }

    private ShopPackage packageWithMenus(String menusJson) {
        ShopPackage sp = new ShopPackage();
        sp.setId(PACKAGE_ID);
        sp.setMenus(menusJson);
        return sp;
    }

    @Test
    void hasMenu_exactMatch_returnsTrue() {
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shopWithPackage());
        when(shopPackageService.getByIdWithTenant(PACKAGE_ID))
                .thenReturn(packageWithMenus("[\"marketing.seckill\",\"goods.list\"]"));

        assertTrue(checker.hasMenu(SHOP_ID, "marketing.seckill"));
    }

    @Test
    void hasMenu_wildcardMatch_returnsTrue() {
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shopWithPackage());
        when(shopPackageService.getByIdWithTenant(PACKAGE_ID))
                .thenReturn(packageWithMenus("[\"marketing.*\"]"));

        assertTrue(checker.hasMenu(SHOP_ID, "marketing.seckill"));
    }

    @Test
    void hasMenu_notPresent_returnsFalse() {
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shopWithPackage());
        when(shopPackageService.getByIdWithTenant(PACKAGE_ID))
                .thenReturn(packageWithMenus("[\"goods.list\"]"));

        assertFalse(checker.hasMenu(SHOP_ID, "marketing.seckill"));
    }

    @Test
    void hasMenu_noPackageAssigned_returnsFalse() {
        Shop shop = new Shop();
        shop.setId(SHOP_ID);
        shop.setPackageId(null);
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shop);

        assertFalse(checker.hasMenu(SHOP_ID, "marketing.seckill"));
    }

    @Test
    void hasMenu_packageRecordMissing_returnsFalse() {
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shopWithPackage());
        when(shopPackageService.getByIdWithTenant(PACKAGE_ID))
                .thenThrow(new TenantAccessDeniedException("id=" + PACKAGE_ID));

        assertFalse(checker.hasMenu(SHOP_ID, "marketing.seckill"));
    }

    @Test
    void requireMenu_locked_throwsBusinessException() {
        Shop shop = new Shop();
        shop.setId(SHOP_ID);
        shop.setPackageId(null);
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shop);

        assertThrows(BusinessException.class, () -> checker.requireMenu(SHOP_ID, "marketing.seckill"));
    }

    @Test
    void requireMenu_allowed_doesNotThrow() {
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shopWithPackage());
        when(shopPackageService.getByIdWithTenant(PACKAGE_ID))
                .thenReturn(packageWithMenus("[\"marketing.seckill\"]"));

        assertDoesNotThrow(() -> checker.requireMenu(SHOP_ID, "marketing.seckill"));
    }

    @Test
    void listMenus_withPackage_returnsParsedList() {
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shopWithPackage());
        when(shopPackageService.getByIdWithTenant(PACKAGE_ID))
                .thenReturn(packageWithMenus("[\"marketing.seckill\",\"goods.list\"]"));

        assertEquals(List.of("marketing.seckill", "goods.list"), checker.listMenus(SHOP_ID));
    }

    @Test
    void listMenus_noPackageAssigned_returnsEmptyList() {
        Shop shop = new Shop();
        shop.setId(SHOP_ID);
        shop.setPackageId(null);
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shop);

        assertTrue(checker.listMenus(SHOP_ID).isEmpty());
    }

    @Test
    void listMenus_packageRecordMissing_returnsEmptyList() {
        when(shopService.getByIdWithTenant(SHOP_ID)).thenReturn(shopWithPackage());
        when(shopPackageService.getByIdWithTenant(PACKAGE_ID))
                .thenThrow(new TenantAccessDeniedException("id=" + PACKAGE_ID));

        assertTrue(checker.listMenus(SHOP_ID).isEmpty());
    }
}
