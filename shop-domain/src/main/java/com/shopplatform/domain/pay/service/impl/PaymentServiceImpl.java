package com.shopplatform.domain.pay.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pay.gateway.WxPayGateway;
import com.shopplatform.domain.pay.gateway.AlipayGateway;
import com.shopplatform.domain.pay.service.PayNotifyLogService;
import com.shopplatform.domain.pay.service.PaymentService;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import com.shopplatform.domain.pay.service.ShopPayConfigService.DecryptedPayConfig;
import com.shopplatform.domain.pay.service.ShopPayConfigService.DecryptedAlipayPayConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String WECHAT = "wechat";
    private static final String ALIPAY = "alipay";

    private final OrderService orderService;
    private final ShopPayConfigService shopPayConfigService;
    private final PayNotifyLogService payNotifyLogService;
    private final WxPayGateway wxPayGateway;
    private final AlipayGateway alipayGateway;
    private final MemberService memberService;
    private final DealerOrderService dealerOrderService;

    public PaymentServiceImpl(OrderService orderService,
                               ShopPayConfigService shopPayConfigService,
                               PayNotifyLogService payNotifyLogService,
                               WxPayGateway wxPayGateway,
                               AlipayGateway alipayGateway,
                               MemberService memberService,
                               DealerOrderService dealerOrderService) {
        this.orderService = orderService;
        this.shopPayConfigService = shopPayConfigService;
        this.payNotifyLogService = payNotifyLogService;
        this.wxPayGateway = wxPayGateway;
        this.alipayGateway = alipayGateway;
        this.memberService = memberService;
        this.dealerOrderService = dealerOrderService;
    }

    @Override
    public PrepayResult createPayment(String channel, Long orderId, String clientIp, String notifyUrl,
                                      String returnUrl) {
        Order order = requireUnpaidOrder(orderId);
        requireSupportedChannel(channel);
        DecryptedPayConfig wechatConfig = WECHAT.equals(channel) ? requireWechatConfig() : null;
        DecryptedAlipayPayConfig alipayConfig = ALIPAY.equals(channel) ? requireAlipayConfig() : null;
        if (!orderService.recordPayMethod(order.getOrderNo(), channel)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不支持发起支付");
        }
        String h5Url;
        if (WECHAT.equals(channel)) {
            h5Url = wxPayGateway.createH5Prepay(wechatConfig, order.getOrderNo(), order.getPayPrice(),
                    "订单" + order.getOrderNo(), notifyUrl, clientIp);
        } else if (ALIPAY.equals(channel)) {
            h5Url = alipayGateway.createWapPrepay(alipayConfig, order.getOrderNo(), order.getPayPrice(),
                    "订单" + order.getOrderNo(), notifyUrl, returnUrl);
        } else {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的支付方式");
        }
        return new PrepayResult(h5Url, order.getOrderNo(), order.getPayPrice());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrepayResult simulatePayment(String channel, Long orderId) {
        requireSupportedChannel(channel);
        Order order = requireUnpaidOrder(orderId);
        String transactionId = "MOCK-" + order.getOrderNo();
        boolean newlyPaid = completeSuccessfulPayment(channel, transactionId, order.getOrderNo());
        if (!newlyPaid) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不支持发起支付");
        }
        return new PrepayResult(null, order.getOrderNo(), order.getPayPrice());
    }

    @Override
    public List<PayChannel> availableChannels() {
        return shopPayConfigService.listEnabledChannels().stream()
                .map(item -> new PayChannel(item.channel(), item.name()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleWechatNotify(String serialNumber, String nonce, String timestamp, String signature,
                                    String body) {
        DecryptedPayConfig config = requireWechatConfig();
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serialNumber)
                .nonce(nonce)
                .timestamp(timestamp)
                .signature(signature)
                .body(body)
                .build();
        Transaction transaction = wxPayGateway.parseNotification(config, requestParam);
        applyTransactionResult(transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlipayNotify(Map<String, String> parameters) {
        DecryptedAlipayPayConfig config = requireAlipayConfig();
        if (!alipayGateway.verifyNotification(config, parameters)
                || !config.appId().equals(parameters.get("app_id"))) {
            throw new BusinessException(ErrorCode.PAY_CALLBACK_INVALID_SIGN, "支付宝回调验签失败");
        }
        String tradeStatus = parameters.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return;
        }
        String orderNo = parameters.get("out_trade_no");
        Order order = orderService.findByOrderNo(orderNo);
        validateAlipayAmount(order, parameters.get("total_amount"));
        completeSuccessfulPayment(ALIPAY, parameters.get("trade_no"), orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileByOrderNo(String orderNo) {
        Order order = orderService.findByOrderNo(orderNo);
        if (order == null || !"unpaid".equals(order.getPayStatus())) {
            return;
        }
        if (ALIPAY.equals(order.getPayMethod())) {
            AlipayGateway.QueryResult result = alipayGateway.queryOrder(requireAlipayConfig(), orderNo);
            if ("TRADE_SUCCESS".equals(result.tradeStatus()) || "TRADE_FINISHED".equals(result.tradeStatus())) {
                validateAlipayAmount(order, result.totalAmount());
                completeSuccessfulPayment(ALIPAY, result.transactionId(), result.outTradeNo());
            }
            return;
        }
        Transaction transaction = wxPayGateway.queryOrder(requireWechatConfig(), orderNo);
        applyTransactionResult(transaction);
    }

    /**
     * 回调与主动查单补偿共用同一个落地方法：先幂等登记（{@code pay_notify_log} 唯一约束兜底并发重复），
     * 登记成功才推进订单状态机——两个入口都可能在同一笔交易上触发，靠这一步保证只处理一次。
     */
    private void applyTransactionResult(Transaction transaction) {
        if (transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS) {
            log.info("交易未完成，暂不处理 outTradeNo={} state={}", transaction.getOutTradeNo(), transaction.getTradeState());
            return;
        }
        completeSuccessfulPayment(WECHAT, transaction.getTransactionId(), transaction.getOutTradeNo());
    }

    /** 模拟支付与真实渠道回调共用成功落地链路，区别仅在于模拟流程不请求外部支付渠道。 */
    private boolean completeSuccessfulPayment(String channel, String transactionId, String orderNo) {
        boolean firstTime = payNotifyLogService.tryMarkProcessed(channel, transactionId, orderNo);
        if (!firstTime) {
            log.info("交易已处理过，本次为重复投递/重复查单 transactionId={}", transactionId);
            return false;
        }
        // 会员消费统计 + 成长值累计（文档三 §3.4 user 表 pay_money/pay_count/growth_value）。
        // 放在 markPaid 成功之后：markPaid 用乐观锁保证只有首次 unpaid→paid 返回 true，
        // 因此这里不会重复累计；同时把异常吞掉只记日志，避免会员统计失败影响支付回调返回 SUCCESS，
        // 统计偏差可由对账任务兜底——支付成功是资金链路，绝不能被会员域异常拖垮。
        boolean newlyPaid = orderService.markPaid(orderNo, transactionId, channel);
        if (newlyPaid) {
            recordPaymentBenefits(orderService.findByOrderNo(orderNo));
        }
        return newlyPaid;
    }

    private Order requireUnpaidOrder(Long orderId) {
        Order order = orderService.getByIdWithTenant(orderId);
        if (!"unpaid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不支持发起支付");
        }
        return order;
    }

    private void recordPaymentBenefits(Order paid) {
        if (paid == null) {
            return;
        }
        try {
            memberService.recordPayment(paid.getUserId(), paid.getPayPrice());
            // 分销佣金：支付成功后生成待结算佣金记录（Sprint 11）
            dealerOrderService.createPending(paid.getId(), paid.getUserId(), paid.getPayPrice());
        } catch (Exception e) {
            log.error("会员消费统计/成长值累计失败 outTradeNo={}", paid.getOrderNo(), e);
        }
    }

    private DecryptedPayConfig requireWechatConfig() {
        return shopPayConfigService.findDecryptedConfig(WECHAT)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "商户尚未配置微信支付"));
    }

    private DecryptedAlipayPayConfig requireAlipayConfig() {
        return shopPayConfigService.findDecryptedAlipayConfig()
                .orElseThrow(() -> new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "商户尚未启用支付宝"));
    }

    private void requireSupportedChannel(String channel) {
        if (!WECHAT.equals(channel) && !ALIPAY.equals(channel)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的支付方式");
        }
    }

    private void validateAlipayAmount(Order order, String totalAmount) {
        if (order == null || !org.springframework.util.StringUtils.hasText(totalAmount)) {
            throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付宝回调订单信息无效");
        }
        try {
            if (order.getPayPrice().compareTo(new BigDecimal(totalAmount)) != 0) {
                throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付宝回调金额不一致");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "支付宝回调金额无效");
        }
    }
}
