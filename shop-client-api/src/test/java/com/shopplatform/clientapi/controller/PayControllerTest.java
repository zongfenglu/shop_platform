package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.PrepayResponse;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pay.service.PaymentService;
import com.shopplatform.framework.security.LoginUserContext;
import com.shopplatform.framework.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayControllerTest {

    private PaymentService paymentService;
    private OrderService orderService;
    private PayController controller;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        orderService = mock(OrderService.class);
        controller = new PayController(paymentService, orderService);
        LoginUserContext.set(new LoginUserContext.LoginUser(20L, 1001L, "user", false));
        TenantContext.set(1001L);

        Order order = new Order();
        order.setId(10L);
        order.setUserId(20L);
        when(orderService.getByIdWithTenant(10L)).thenReturn(order);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void prepay_usesSimulationWhenEnabled() {
        ReflectionTestUtils.setField(controller, "mockEnabled", true);
        when(paymentService.simulatePayment("wechat", 10L)).thenReturn(
                new PaymentService.PrepayResult(null, "ORDER123", new BigDecimal("99.00")));

        PrepayResponse response = controller.prepay(10L, "wechat", mock(HttpServletRequest.class)).getData();

        assertTrue(response.simulated());
        verify(paymentService).simulatePayment("wechat", 10L);
    }

    @Test
    void prepay_usesWechatGatewayWhenSimulationIsDisabled() {
        ReflectionTestUtils.setField(controller, "mockEnabled", false);
        ReflectionTestUtils.setField(controller, "notifyBaseUrl", "https://h5.example.com");
        ReflectionTestUtils.setField(controller, "returnBaseUrl", "https://store.example.com");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(paymentService.createPayment("wechat", 10L, "127.0.0.1",
                "https://h5.example.com/api/pay/notify/wechat/1001",
                "https://store.example.com/pages/order/list?_shopId=1001")).thenReturn(
                new PaymentService.PrepayResult("https://wx.example/pay", "ORDER123", new BigDecimal("99.00")));

        PrepayResponse response = controller.prepay(10L, "wechat", request).getData();

        assertFalse(response.simulated());
        verify(paymentService).createPayment("wechat", 10L, "127.0.0.1",
                "https://h5.example.com/api/pay/notify/wechat/1001",
                "https://store.example.com/pages/order/list?_shopId=1001");
    }
}
