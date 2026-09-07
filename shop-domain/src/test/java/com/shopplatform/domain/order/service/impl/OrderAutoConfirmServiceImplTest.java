package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderPackage;
import com.shopplatform.domain.order.service.OrderPackageService;
import com.shopplatform.domain.order.service.OrderService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderAutoConfirmServiceImplTest {

    @Test
    void confirmOverdue_confirmsWhenFirstPackageOlderThanWindow() {
        OrderService orderService = mock(OrderService.class);
        OrderPackageService packageService = mock(OrderPackageService.class);
        Order order = new Order();
        order.setId(88L);
        order.setShopId(1001L);
        when(orderService.list(any(Wrapper.class))).thenReturn(List.of(order));
        OrderPackage pkg = new OrderPackage();
        pkg.setCreateTime(LocalDateTime.now().minusDays(16));
        when(packageService.listByOrderId(88L)).thenReturn(List.of(pkg));

        int n = new OrderAutoConfirmServiceImpl(orderService, packageService, 15).confirmOverdue();
        assertEquals(1, n);
        verify(orderService).confirmReceipt(88L);
    }

    @Test
    void confirmOverdue_skipsRecentShipment() {
        OrderService orderService = mock(OrderService.class);
        OrderPackageService packageService = mock(OrderPackageService.class);
        Order order = new Order();
        order.setId(88L);
        order.setShopId(1001L);
        when(orderService.list(any(Wrapper.class))).thenReturn(List.of(order));
        OrderPackage pkg = new OrderPackage();
        pkg.setCreateTime(LocalDateTime.now().minusDays(1));
        when(packageService.listByOrderId(88L)).thenReturn(List.of(pkg));

        int n = new OrderAutoConfirmServiceImpl(orderService, packageService, 15).confirmOverdue();
        assertEquals(0, n);
        verify(orderService, never()).confirmReceipt(any());
    }
}
