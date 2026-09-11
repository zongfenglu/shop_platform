package com.shopplatform.domain.pay.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pay.gateway.WxPayGateway;
import com.shopplatform.domain.pay.service.PayNotifyLogService;
import com.shopplatform.domain.pay.service.PaymentService;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import com.shopplatform.domain.pay.service.ShopPayConfigService.DecryptedPayConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String CHANNEL = "wechat";

    private final OrderService orderService;
    private final ShopPayConfigService shopPayConfigService;
    private final PayNotifyLogService payNotifyLogService;
    private final WxPayGateway wxPayGateway;
    private final MemberService memberService;
    private final DealerOrderService dealerOrderService;

    public PaymentServiceImpl(OrderService orderService,
                               ShopPayConfigService shopPayConfigService,
                               PayNotifyLogService payNotifyLogService,
                               WxPayGateway wxPayGateway,
                               MemberService memberService,
                               DealerOrderService dealerOrderService) {
        this.orderService = orderService;
        this.shopPayConfigService = shopPayConfigService;
        this.payNotifyLogService = payNotifyLogService;
        this.wxPayGateway = wxPayGateway;
        this.memberService = memberService;
        this.dealerOrderService = dealerOrderService;
    }

    @Override
    public PrepayResult createPayment(Long orderId, String clientIp, String notifyUrl) {
        Order order = requireUnpaidOrder(orderId);
        DecryptedPayConfig config = requireConfig();
        String h5Url = wxPayGateway.createH5Prepay(config, order.getOrderNo(), order.getPayPrice(),
                "订单" + order.getOrderNo(), notifyUrl, clientIp);
        return new PrepayResult(h5Url, order.getOrderNo(), order.getPayPrice());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrepayResult simulatePayment(Long orderId) {
        Order order = requireUnpaidOrder(orderId);
        String transactionId = "MOCK-" + order.getOrderNo();
        boolean newlyPaid = orderService.markPaid(order.getOrderNo(), transactionId, "mock");
        if (!newlyPaid) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不支持发起支付");
        }
        recordPaymentBenefits(order);
        return new PrepayResult(null, order.getOrderNo(), order.getPayPrice());
    }

    @Override
    public void handleWechatNotify(String serialNumber, String nonce, String timestamp, String signature,
                                    String body) {
        DecryptedPayConfig config = requireConfig();
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
    public void reconcileByOrderNo(String orderNo) {
        Order order = orderService.findByOrderNo(orderNo);
        if (order == null || !"unpaid".equals(order.getPayStatus())) {
            return;
        }
        DecryptedPayConfig config = requireConfig();
        Transaction transaction = wxPayGateway.queryOrder(config, orderNo);
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
        boolean firstTime = payNotifyLogService.tryMarkProcessed(
                CHANNEL, transaction.getTransactionId(), transaction.getOutTradeNo());
        if (!firstTime) {
            log.info("交易已处理过，本次为重复投递/重复查单 transactionId={}", transaction.getTransactionId());
            return;
        }
        // 会员消费统计 + 成长值累计（文档三 §3.4 user 表 pay_money/pay_count/growth_value）。
        // 放在 markPaid 成功之后：markPaid 用乐观锁保证只有首次 unpaid→paid 返回 true，
        // 因此这里不会重复累计；同时把异常吞掉只记日志，避免会员统计失败影响支付回调返回 SUCCESS，
        // 统计偏差可由对账任务兜底——支付成功是资金链路，绝不能被会员域异常拖垮。
        boolean newlyPaid = orderService.markPaid(transaction.getOutTradeNo(), transaction.getTransactionId(), CHANNEL);
        if (newlyPaid) {
            recordPaymentBenefits(orderService.findByOrderNo(transaction.getOutTradeNo()));
        }
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

    private DecryptedPayConfig requireConfig() {
        return shopPayConfigService.findDecryptedConfig(CHANNEL)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "商户尚未配置微信支付"));
    }
}
