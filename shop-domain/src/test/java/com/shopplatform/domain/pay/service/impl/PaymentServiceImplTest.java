package com.shopplatform.domain.pay.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pay.gateway.WxPayGateway;
import com.shopplatform.domain.pay.gateway.AlipayGateway;
import com.shopplatform.domain.pay.service.PayNotifyLogService;
import com.shopplatform.domain.pay.service.PaymentService;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

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
    private AlipayGateway alipayGateway;
    private MemberService memberService;
    private DealerOrderService dealerOrderService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        shopPayConfigService = mock(ShopPayConfigService.class);
        payNotifyLogService = mock(PayNotifyLogService.class);
        wxPayGateway = mock(WxPayGateway.class);
        alipayGateway = mock(AlipayGateway.class);
        memberService = mock(MemberService.class);
        dealerOrderService = mock(DealerOrderService.class);
        paymentService = new PaymentServiceImpl(orderService, shopPayConfigService, payNotifyLogService,
                wxPayGateway, alipayGateway, memberService, dealerOrderService);
    }

    @Test
    void simulatePayment_marksOrderPaidAndRunsPostPaymentBenefits() {
        Order order = unpaidOrder();
        when(orderService.getByIdWithTenant(10L)).thenReturn(order);
        when(payNotifyLogService.tryMarkProcessed("wechat", "MOCK-ORDER123", "ORDER123")).thenReturn(true);
        when(orderService.markPaid("ORDER123", "MOCK-ORDER123", "wechat")).thenReturn(true);
        when(orderService.findByOrderNo("ORDER123")).thenReturn(order);

        PaymentService.PrepayResult result = paymentService.simulatePayment("wechat", 10L);

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

        assertThrows(BusinessException.class, () -> paymentService.simulatePayment("wechat", 10L));

        verify(orderService, never()).markPaid(any(), any(), any());
        verifyNoInteractions(memberService, dealerOrderService);
    }

    @Test
    void createPayment_routesAlipayThroughIjpayGateway() {
        Order order = unpaidOrder();
        ShopPayConfigService.DecryptedAlipayPayConfig config = new ShopPayConfigService.DecryptedAlipayPayConfig(
                1001L, "app-1", "private", "public", "https://openapi.alipay.com/gateway.do");
        when(orderService.getByIdWithTenant(10L)).thenReturn(order);
        when(orderService.recordPayMethod("ORDER123", "alipay")).thenReturn(true);
        when(shopPayConfigService.findDecryptedAlipayConfig()).thenReturn(Optional.of(config));
        when(alipayGateway.createWapPrepay(config, "ORDER123", new BigDecimal("99.00"),
                "订单ORDER123", "https://api.example/notify", "https://h5.example/orders"))
                .thenReturn("https://openapi.alipay.com/gateway.do?signed=1");

        PaymentService.PrepayResult result = paymentService.createPayment(
                "alipay", 10L, "127.0.0.1", "https://api.example/notify", "https://h5.example/orders");

        assertEquals("https://openapi.alipay.com/gateway.do?signed=1", result.h5Url());
        verify(orderService).recordPayMethod("ORDER123", "alipay");
        verifyNoInteractions(wxPayGateway);
    }

    @Test
    void alipayNotify_verifiesAmountAndUsesSharedPaymentCompletion() {
        Order order = unpaidOrder();
        ShopPayConfigService.DecryptedAlipayPayConfig config = new ShopPayConfigService.DecryptedAlipayPayConfig(
                1001L, "app-1", "private", "public", "https://openapi.alipay.com/gateway.do");
        Map<String, String> parameters = Map.of(
                "app_id", "app-1", "trade_status", "TRADE_SUCCESS", "out_trade_no", "ORDER123",
                "trade_no", "ALI-123", "total_amount", "99.00", "sign", "signature");
        when(shopPayConfigService.findDecryptedAlipayConfig()).thenReturn(Optional.of(config));
        when(alipayGateway.verifyNotification(config, parameters)).thenReturn(true);
        when(orderService.findByOrderNo("ORDER123")).thenReturn(order);
        when(payNotifyLogService.tryMarkProcessed("alipay", "ALI-123", "ORDER123")).thenReturn(true);
        when(orderService.markPaid("ORDER123", "ALI-123", "alipay")).thenReturn(true);

        paymentService.handleAlipayNotify(parameters);

        verify(orderService).markPaid("ORDER123", "ALI-123", "alipay");
        verify(memberService).recordPayment(20L, new BigDecimal("99.00"));
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
