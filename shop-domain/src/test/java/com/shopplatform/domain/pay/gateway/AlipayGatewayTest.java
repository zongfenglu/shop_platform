package com.shopplatform.domain.pay.gateway;

import com.shopplatform.domain.pay.service.ShopPayConfigService.DecryptedAlipayPayConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AlipayGatewayTest {

    @Test
    void createWapPrepay_returnsSignedGatewayUrl() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair application = generator.generateKeyPair();
        KeyPair alipay = generator.generateKeyPair();
        DecryptedAlipayPayConfig config = new DecryptedAlipayPayConfig(
                1001L, "2026000000000001",
                Base64.getEncoder().encodeToString(application.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(alipay.getPublic().getEncoded()),
                "https://openapi.alipay.com/gateway.do");

        String url = new AlipayGateway().createWapPrepay(
                config, "ORDER123", new BigDecimal("99.00"), "订单ORDER123",
                "https://api.example.com/api/pay/notify/alipay/1001",
                "https://h5.example.com/pages/order/list?_shopId=1001");

        assertTrue(url.startsWith("https://openapi.alipay.com/gateway.do?"));
        assertTrue(url.contains("sign="));
        assertTrue(url.contains("alipay.trade.wap.pay"));
    }
}
