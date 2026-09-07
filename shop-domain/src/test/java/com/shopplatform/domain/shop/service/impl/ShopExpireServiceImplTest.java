package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.service.ShopService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopExpireServiceImplTest {

    @Test
    void expireDue_marksTrialAndNormalPastExpireTime() {
        ShopService shopService = mock(ShopService.class);
        Shop due = new Shop();
        due.setId(1001L);
        due.setStatus(ShopStatus.NORMAL.getCode());
        due.setExpireTime(LocalDateTime.now().minusDays(1));
        when(shopService.list(any(Wrapper.class))).thenReturn(List.of(due));

        int changed = new ShopExpireServiceImpl(shopService).expireDue();
        assertEquals(1, changed);
        assertEquals(ShopStatus.EXPIRED.getCode(), due.getStatus());
        verify(shopService).updateById(due);
    }
}
