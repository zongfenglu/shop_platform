package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.service.ShopService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopExpireRemindServiceImplTest {

    @Test
    void remindDue_writesLogForSevenDayWindow() {
        ShopService shopService = mock(ShopService.class);
        SysLogService sysLogService = mock(SysLogService.class);
        Shop shop = new Shop();
        shop.setId(1001L);
        shop.setName("演示店");
        shop.setStatus(ShopStatus.NORMAL.getCode());
        shop.setExpireTime(LocalDate.now().plusDays(7).atTime(12, 0));
        when(shopService.list(any(Wrapper.class))).thenReturn(List.of(shop));
        when(sysLogService.count(any())).thenReturn(0L);

        int n = new ShopExpireRemindServiceImpl(shopService, sysLogService).remindDue();
        assertEquals(1, n);
        verify(sysLogService).record(eq(1001L), eq(0), eq(0L), eq("system"), eq(false),
                eq("shop-expire-remind"), any(), eq(null));
    }

    @Test
    void remindDue_skipsAlreadyRemindedToday() {
        ShopService shopService = mock(ShopService.class);
        SysLogService sysLogService = mock(SysLogService.class);
        Shop shop = new Shop();
        shop.setId(1001L);
        shop.setExpireTime(LocalDate.now().plusDays(3).atTime(12, 0));
        when(shopService.list(any(Wrapper.class))).thenReturn(List.of(shop));
        when(sysLogService.count(any())).thenReturn(1L);

        int n = new ShopExpireRemindServiceImpl(shopService, sysLogService).remindDue();
        assertEquals(0, n);
        verify(sysLogService, never()).record(any(), anyInt(), any(), any(), anyBoolean(), any(), any(), any());
    }
}
