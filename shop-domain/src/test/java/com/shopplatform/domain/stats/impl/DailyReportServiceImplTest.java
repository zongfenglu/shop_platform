package com.shopplatform.domain.stats.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.stats.StatGoodsDailyService;
import com.shopplatform.domain.stats.StatShopDailyService;
import com.shopplatform.domain.stats.entity.StatShopDaily;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyReportServiceImplTest {

    @Test
    void summarize_writesPaidAndOrderCounts() {
        ShopService shopService = mock(ShopService.class);
        OrderService orderService = mock(OrderService.class);
        OrderGoodsService orderGoodsService = mock(OrderGoodsService.class);
        AfterSaleService afterSaleService = mock(AfterSaleService.class);
        MemberService memberService = mock(MemberService.class);
        StatShopDailyService shopDaily = mock(StatShopDailyService.class);
        StatGoodsDailyService goodsDaily = mock(StatGoodsDailyService.class);

        when(shopService.listAllShopIds()).thenReturn(List.of(1001L));
        Order created = new Order();
        created.setId(1L);
        created.setCreateTime(LocalDateTime.now().minusDays(1));
        Order paid = new Order();
        paid.setId(2L);
        paid.setUserId(9L);
        paid.setPayStatus("paid");
        paid.setPayPrice(new BigDecimal("12.50"));
        paid.setPayTime(LocalDate.now().minusDays(1).atTime(10, 0));
        when(orderService.list(any(Wrapper.class))).thenReturn(List.of(created), List.of(paid), List.of(paid));
        when(afterSaleService.list(any(Wrapper.class))).thenReturn(List.of());
        when(memberService.count(any())).thenReturn(1L);
        when(shopDaily.getOne(any())).thenReturn(null);
        when(goodsDaily.getOne(any())).thenReturn(null);
        OrderGoods line = new OrderGoods();
        line.setGoodsId(77L);
        line.setTotalNum(2);
        line.setTotalPrice(new BigDecimal("12.50"));
        when(orderGoodsService.listByOrderId(2L)).thenReturn(List.of(line));

        int n = new DailyReportServiceImpl(
                shopService, orderService, orderGoodsService, afterSaleService,
                memberService, shopDaily, goodsDaily)
                .summarize(LocalDate.now().minusDays(1));
        assertEquals(1, n);

        ArgumentCaptor<StatShopDaily> captor = ArgumentCaptor.forClass(StatShopDaily.class);
        verify(shopDaily).save(captor.capture());
        StatShopDaily row = captor.getValue();
        assertEquals(1001L, row.getShopId());
        assertEquals(1, row.getOrderCount());
        assertEquals(1, row.getPayCount());
        assertEquals(new BigDecimal("12.50"), row.getPayAmount());
        assertEquals(1, row.getNewUser());
        assertEquals(1, row.getActiveUser());
        assertEquals(0, row.getUv());
    }
}
