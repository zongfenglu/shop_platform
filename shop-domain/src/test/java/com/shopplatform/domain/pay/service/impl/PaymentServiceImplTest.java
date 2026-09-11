package com.shopplatform.domain.pay.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pay.gateway.WxPayGateway;
import com.shopplatform.domain.pay.service.PayNotifyLogService;
import com.shopplatform.domain.pay.service.PaymentService;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    private OrderService orderService;
    private ShopPayConfigService shopPayConfigService;
    private PayNotifyLogService payNotifyLogService;
    private WxPayGateway wxPayGateway;
    private MemberService memberService;
    private DealerOrderService dealerOrderService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        shopPayConfigService = mock(ShopPayConfigService.class);
        payNotifyLogService = mock(PayNotifyLogService.class);
        wxPayGateway = mock(WxPayGateway.class);
        memberService = mock(MemberService.class);
        dealerOrderService = mock(DealerOrderService.class);
        paymentService = new PaymentServiceImpl(orderService, shopPayConfigService, payNotifyLogService,
                wxPayGateway, memberService, dealerOrderService);
    }

    @Test
    void simulatePayment_marksOrderPaidAndRunsPostPaymentBenefits() {
        Order order = unpaidOrder();
        when(orderService.getByIdWithTenant(10L)).thenReturn(order);
        when(payNotifyLogService.tryMarkProcessed("wechat", "MOCK-ORDER123", "ORDER123")).thenReturn(true);
        when(orderService.markPaid("ORDER123", "MOCK-ORDER123", "wechat")).thenReturn(true);
        when(orderService.findByOrderNo("ORDER123")).thenReturn(order);

        PaymentService.PrepayResult result = paymentService.simulatePayment(10L);

        assertNull(result.h5Url());
        assertEquals("ORDER123", result.orderNo());
        assertEquals(new BigDecimal("99.00"), result.payPrice());
        verify(memberService).recordPayment(20L, new BigDecimal("99.00"));
        verify(dealerOrderService).createPending(10L, 20L, new BigDecimal("99.00"));
        verify(payNotifyLogService).tryMarkProcessed("wechat", "MOCK-ORDER123", "ORDER123");
        verifyNoInteractions(shopPayConfigService, wxPayGateway);
    }

    @Test
    void simulatePayment_rejectsOrderThatIsNoLongerUnpaid() {
        Order order = unpaidOrder();
        order.setPayStatus("paid");
        when(orderService.getByIdWithTenant(10L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> paymentService.simulatePayment(10L));

        verify(orderService, never()).markPaid(any(), any(), any());
        verifyNoInteractions(memberService, dealerOrderService);
    }

    private static Order unpaidOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderNo("ORDER123");
        order.setUserId(20L);
        order.setPayPrice(new BigDecimal("99.00"));
        order.setPayStatus("unpaid");
        return order;
    }
}
