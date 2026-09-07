package com.shopplatform.domain.pay.service;

import java.math.BigDecimal;

/**
 * 支付编排服务：发起支付、处理回调、主动查单补偿三个入口共用同一套"验证交易 -> 幂等登记 -> 订单状态流转"逻辑，
 * 见 {@link com.shopplatform.domain.pay.service.impl.PaymentServiceImpl#applyTransactionResult}。
 */
public interface PaymentService {

    /** 发起支付，{@code notifyUrl} 由调用方（shop-client-api）拼出自己的公网回调地址传入——领域层不关心部署域名。 */
    PrepayResult createPayment(Long orderId, String clientIp, String notifyUrl);

    /** 处理微信支付回调：验签、AEAD解密、幂等登记、订单状态流转，调用前 TenantContext 必须已按 URL 中的 shopId 设好。 */
    void handleWechatNotify(String serialNumber, String nonce, String timestamp, String signature, String body);

    /** 主动查单补偿（回调丢失兜底），定时任务/手工触发均可调用。 */
    void reconcileByOrderNo(String orderNo);

    record PrepayResult(String h5Url, String orderNo, BigDecimal payPrice) {
    }
}
