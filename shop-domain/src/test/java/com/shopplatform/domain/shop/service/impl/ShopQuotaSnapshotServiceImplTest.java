package com.shopplatform.domain.shop.service.impl;

import com.shopplatform.domain.file.service.StorageService;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.shop.entity.ShopQuotaUsage;
import com.shopplatform.domain.shop.service.ShopQuotaUsageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.shop.service.StoreUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopQuotaSnapshotServiceImplTest {

    @Test
    void snapshotAll_writesGoodsStaffAndStorage() {
        ShopService shopService = mock(ShopService.class);
        ShopQuotaUsageService usageService = mock(ShopQuotaUsageService.class);
        GoodsService goodsService = mock(GoodsService.class);
        StoreUserService storeUserService = mock(StoreUserService.class);
        StorageService storageService = mock(StorageService.class);
        when(shopService.listAllShopIds()).thenReturn(List.of(1001L));
        when(goodsService.count()).thenReturn(3L);
        when(storeUserService.count(any())).thenReturn(1L);
        when(storageService.directorySize(1001L)).thenReturn(2048L);
        when(usageService.getOne(any())).thenReturn(null);

        int ok = new ShopQuotaSnapshotServiceImpl(
                shopService, usageService, goodsService, storeUserService, storageService).snapshotAll();
        assertEquals(1, ok);

        ArgumentCaptor<ShopQuotaUsage> captor = ArgumentCaptor.forClass(ShopQuotaUsage.class);
        verify(usageService).save(captor.capture());
        ShopQuotaUsage row = captor.getValue();
        assertEquals(1001L, row.getShopId());
        assertEquals(3, row.getGoodsCount());
        assertEquals(1, row.getStaffCount());
        assertEquals(2048L, row.getStorageBytes());
        assertEquals(0, row.getSmsMonthUsed());
    }
}
