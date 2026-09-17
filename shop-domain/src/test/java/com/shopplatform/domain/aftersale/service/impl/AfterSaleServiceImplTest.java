package com.shopplatform.domain.aftersale.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.aftersale.RefundCalculator;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.aftersale.service.RefundLogService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pay.gateway.AlipayGateway;
import com.shopplatform.domain.pay.gateway.WxPayGateway;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleServiceImplTest {

    private OrderService orderService;
    private OrderGoodsService orderGoodsService;
    private RefundCalculator refundCalculator;
    private AfterSaleServiceImpl service;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        orderGoodsService = mock(OrderGoodsService.class);
        refundCalculator = mock(RefundCalculator.class);
        service = org.mockito.Mockito.spy(new AfterSaleServiceImpl(
                orderService,
                orderGoodsService,
                mock(GoodsSkuService.class),
                refundCalculator,
                mock(RefundLogService.class),
                mock(ShopPayConfigService.class),
                mock(WxPayGateway.class),
                mock(AlipayGateway.class),
                new ObjectMapper()));
    }

    @Test
    void apply_paidPendingOrder_allowsRefundOnly() {
        Order order = order("paid", "normal", "pending");
        OrderGoods goods = orderGoods();
        when(orderService.getByIdWithTenant(10L)).thenReturn(order);
        when(orderGoodsService.getByIdWithTenant(20L)).thenReturn(goods);
        when(refundCalculator.calculate(goods, 1)).thenReturn(
                new RefundCalculator.RefundResult(new BigDecimal("88.00"), Map.of()));
        doReturn(true).when(service).save(any(AfterSale.class));

        AfterSale result = service.apply(command("refund_only"));

        assertEquals("refund_only", result.getType());
        assertEquals(new BigDecimal("88.00"), result.getRefundAmount());
        assertEquals("applying", goods.getRefundStatus());
        verify(orderGoodsService).updateById(goods);
    }

    @Test
    void apply_paidPendingOrder_rejectsReturnRefund() {
        when(orderService.getByIdWithTenant(10L)).thenReturn(order("paid", "normal", "pending"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.apply(command("return_refund")));

        assertEquals("商品尚未收货，请选择仅退款", error.getMessage());
    }

    @Test
    void apply_shippedOrder_requiresReceiptFirst() {
        when(orderService.getByIdWithTenant(10L)).thenReturn(order("paid", "normal", "shipped"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.apply(command("refund_only")));

        assertEquals("商品运输中，请确认收货后再申请售后", error.getMessage());
    }

    @Test
    void apply_unpaidOrder_rejectsAfterSale() {
        when(orderService.getByIdWithTenant(10L)).thenReturn(order("unpaid", "normal", "pending"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.apply(command("refund_only")));

        assertEquals("订单尚未支付，不能申请售后", error.getMessage());
    }

    private AfterSaleService.ApplyCommand command(String type) {
        return new AfterSaleService.ApplyCommand(10L, 20L, 30L, type, "不想要了", "", null, 1);
    }

    private Order order(String payStatus, String orderStatus, String deliveryStatus) {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(30L);
        order.setOrderNo("202609141234567890");
        order.setPayStatus(payStatus);
        order.setOrderStatus(orderStatus);
        order.setDeliveryStatus(deliveryStatus);
        return order;
    }

    private OrderGoods orderGoods() {
        OrderGoods goods = new OrderGoods();
        goods.setId(20L);
        goods.setOrderId(10L);
        goods.setTotalNum(1);
        goods.setRefundStatus("none");
        return goods;
    }
}
