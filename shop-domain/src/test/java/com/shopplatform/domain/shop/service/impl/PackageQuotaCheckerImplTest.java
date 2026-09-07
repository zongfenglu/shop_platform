package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.mapper.DiyPageMapper;
import com.shopplatform.domain.goods.mapper.GoodsMapper;
import com.shopplatform.domain.offlinestore.mapper.OfflineStoreMapper;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.mapper.StoreUserMapper;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageQuotaCheckerImplTest {

    private static final Long SHOP_ID = 1001L;

    private ShopService shopService;
    private ShopPackageService shopPackageService;
    private GoodsMapper goodsMapper;
    private OfflineStoreMapper offlineStoreMapper;
    private PackageQuotaCheckerImpl checker;

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        shopPackageService = mock(ShopPackageService.class);
        goodsMapper = mock(GoodsMapper.class);
        StoreUserMapper storeUserMapper = mock(StoreUserMapper.class);
        offlineStoreMapper = mock(OfflineStoreMapper.class);
        DiyPageMapper diyPageMapper = mock(DiyPageMapper.class);
        checker = new PackageQuotaCheckerImpl(
                shopService, shopPackageService, goodsMapper, storeUserMapper,
                offlineStoreMapper, diyPageMapper, new ObjectMapper());
        TenantContext.set(SHOP_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void requireGoods_noPackage_isUnlimited() {
        Shop shop = new Shop();
        shop.setId(SHOP_ID);
        when(shopService.getOne(any(Wrapper.class))).thenReturn(shop);
        when(goodsMapper.selectCount(any())).thenReturn(99L);
        assertDoesNotThrow(checker::requireGoods);
    }

    @Test
    void requireGoods_negativeMax_isUnlimited() {
        bindQuota("{\"goods_max\":-1}");
        when(goodsMapper.selectCount(any())).thenReturn(999L);
        assertDoesNotThrow(checker::requireGoods);
    }

    @Test
    void requireGoods_usedBelowMax_allows() {
        bindQuota("{\"goods_max\":5}");
        when(goodsMapper.selectCount(any())).thenReturn(4L);
        assertDoesNotThrow(checker::requireGoods);
    }

    @Test
    void requireGoods_usedAtMax_blocks() {
        bindQuota("{\"goods_max\":5}");
        when(goodsMapper.selectCount(any())).thenReturn(5L);
        BusinessException ex = assertThrows(BusinessException.class, checker::requireGoods);
        assertEquals(ErrorCode.QUOTA_EXCEEDED.getCode(), ex.getCode());
    }

    @Test
    void requireStore_zeroMax_blocks() {
        bindQuota("{\"store_max\":0}");
        when(offlineStoreMapper.selectCount(any())).thenReturn(0L);
        BusinessException ex = assertThrows(BusinessException.class, checker::requireStore);
        assertEquals(ErrorCode.QUOTA_EXCEEDED.getCode(), ex.getCode());
    }

    private void bindQuota(String quotaJson) {
        Shop shop = new Shop();
        shop.setId(SHOP_ID);
        shop.setPackageId(14001L);
        ShopPackage pkg = new ShopPackage();
        pkg.setId(14001L);
        pkg.setQuota(quotaJson);
        when(shopService.getOne(any(Wrapper.class))).thenReturn(shop);
        when(shopPackageService.getOne(any(Wrapper.class))).thenReturn(pkg);
    }
}
