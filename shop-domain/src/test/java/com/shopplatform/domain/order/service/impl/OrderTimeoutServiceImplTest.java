package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderTimeoutServiceImplTest {

    @Test
    void closeOverdue_cancelsUnpaidPastWindow() {
        OrderService orderService = mock(OrderService.class);
        Order order = new Order();
        order.setId(5L);
        order.setShopId(1001L);
        order.setCreateTime(LocalDateTime.now().minusMinutes(40));
        when(orderService.list(any(Wrapper.class))).thenReturn(List.of(order));
        when(orderService.cancel(eq(5L), any())).thenReturn(true);

        int n = new OrderTimeoutServiceImpl(orderService, 30).closeOverdue();
        assertEquals(1, n);
        verify(orderService).cancel(5L, "超时未支付自动取消");
    }
}
