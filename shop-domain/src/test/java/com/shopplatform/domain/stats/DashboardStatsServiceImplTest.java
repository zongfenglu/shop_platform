package com.shopplatform.domain.stats;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.shop.service.ShopOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardStatsServiceImplTest {

    private OrderService orderService;
    private AfterSaleService afterSaleService;
    private GoodsService goodsService;
    private ShopOrderService shopOrderService;
    private DashboardStatsServiceImpl service;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        afterSaleService = mock(AfterSaleService.class);
        goodsService = mock(GoodsService.class);
        shopOrderService = mock(ShopOrderService.class);
        service = new DashboardStatsServiceImpl(orderService, afterSaleService, goodsService, shopOrderService);
    }

    @Test
    void shopOverview_aggregatesTodayAndWeek() {
        Order paidToday = new Order();
        paidToday.setPayStatus("paid");
        paidToday.setPayPrice(new BigDecimal("88.50"));
        paidToday.setPayTime(LocalDateTime.now());
        when(orderService.list(any(Wrapper.class))).thenReturn(List.of(paidToday));
        when(orderService.count(any())).thenReturn(2L, 1L);
        when(afterSaleService.count(any())).thenReturn(3L);
        when(goodsService.count()).thenReturn(10L);
        when(goodsService.count(any())).thenReturn(7L);

        var overview = service.shopOverview();
        assertEquals(new BigDecimal("88.50"), overview.todayGmv());
        assertEquals(1L, overview.todayOrderCount());
        assertEquals(2L, overview.unpaidCount());
        assertEquals(1L, overview.pendingShipCount());
        assertEquals(3L, overview.afterSaleOpenCount());
        assertEquals(10L, overview.goodsTotal());
        assertEquals(7L, overview.goodsOnSale());
        assertEquals(7, overview.last7Days().size());
    }

    @Test
    void platformOverview_sumsWeekAndPendingShopOrders() {
        Order paidToday = new Order();
        paidToday.setPayStatus("paid");
        paidToday.setPayPrice(new BigDecimal("12.00"));
        paidToday.setPayTime(LocalDateTime.now());
        when(orderService.list(any(Wrapper.class))).thenReturn(List.of(paidToday));
        when(shopOrderService.count(any())).thenReturn(4L);

        var overview = service.platformOverview();
        assertEquals(new BigDecimal("12.00"), overview.todayGmv());
        assertEquals(1L, overview.todayOrderCount());
        assertEquals(4L, overview.pendingShopOrders());
        assertEquals(new BigDecimal("12.00"), overview.last7DaysGmv());
        assertEquals(7, overview.last7Days().size());
    }
}
