package com.shopplatform.domain.aftersale.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleTimeoutServiceImplTest {

    @Test
    void approveOverdue_approvesApplyingRows() {
        AfterSaleService afterSaleService = mock(AfterSaleService.class);
        AfterSale row = new AfterSale();
        row.setId(9L);
        row.setShopId(1001L);
        when(afterSaleService.list(any(Wrapper.class))).thenReturn(List.of(row));

        int n = new AfterSaleTimeoutServiceImpl(afterSaleService, 7).approveOverdue();
        assertEquals(1, n);
        verify(afterSaleService).approve(9L, "超时自动同意");
    }
}
