package com.shopplatform.domain.aftersale.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.aftersale.RefundCalculator;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.entity.RefundLog;
import com.shopplatform.domain.aftersale.mapper.AfterSaleMapper;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.aftersale.service.RefundLogService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pay.gateway.WxPayGateway;
import com.shopplatform.domain.pay.gateway.AlipayGateway;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import com.shopplatform.domain.pay.service.ShopPayConfigService.DecryptedPayConfig;
import com.wechat.pay.java.service.refund.model.Refund;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AfterSaleServiceImpl extends ServiceImpl<AfterSaleMapper, AfterSale> implements AfterSaleService {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String WECHAT = "wechat";
    private static final String ALIPAY = "alipay";

    private final OrderService orderService;
    private final OrderGoodsService orderGoodsService;
    private final GoodsSkuService goodsSkuService;
    private final RefundCalculator refundCalculator;
    private final RefundLogService refundLogService;
    private final ShopPayConfigService shopPayConfigService;
    private final WxPayGateway wxPayGateway;
    private final AlipayGateway alipayGateway;
    private final ObjectMapper objectMapper;

    public AfterSaleServiceImpl(OrderService orderService,
                                OrderGoodsService orderGoodsService,
                                GoodsSkuService goodsSkuService,
                                RefundCalculator refundCalculator,
                                RefundLogService refundLogService,
                                ShopPayConfigService shopPayConfigService,
                                WxPayGateway wxPayGateway,
                                AlipayGateway alipayGateway,
                                ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.orderGoodsService = orderGoodsService;
        this.goodsSkuService = goodsSkuService;
        this.refundCalculator = refundCalculator;
        this.refundLogService = refundLogService;
        this.shopPayConfigService = shopPayConfigService;
        this.wxPayGateway = wxPayGateway;
        this.alipayGateway = alipayGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSale apply(ApplyCommand cmd) {
        Order order = orderService.getByIdWithTenant(cmd.orderId());
        if (!order.getUserId().equals(cmd.userId())) {
            // 同租户内的越权：见 GoodsCommentServiceImpl 同类修复注释。
            throw new TenantAccessDeniedException("该订单不属于当前用户，无法申请售后");
        }
        if (!"confirmed".equals(order.getReceiptStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单尚未确认收货，不能申请售后");
        }

        OrderGoods orderGoods = orderGoodsService.getByIdWithTenant(cmd.orderGoodsId());
        if (!orderGoods.getOrderId().equals(cmd.orderId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "订单商品行与订单不匹配");
        }
        if (!"none".equals(orderGoods.getRefundStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "该商品已有进行中或已完成的售后单");
        }

        int refundNum = cmd.refundNum() == null ? orderGoods.getTotalNum() : cmd.refundNum();
        RefundCalculator.RefundResult refundResult = refundCalculator.calculate(orderGoods, refundNum);

        AfterSale afterSale = new AfterSale();
        afterSale.setOrderId(cmd.orderId());
        afterSale.setOrderGoodsId(cmd.orderGoodsId());
        afterSale.setUserId(cmd.userId());
        afterSale.setType(cmd.type());
        afterSale.setApplyReason(cmd.applyReason());
        afterSale.setApplyDesc(cmd.applyDesc());
        afterSale.setImages(toJson(cmd.images() == null ? List.of() : cmd.images()));
        afterSale.setRefundNum(refundNum);
        afterSale.setRefundAmount(refundResult.refundAmount());
        afterSale.setRefundDetail(toJson(refundResult.refundDetail()));
        afterSale.setStatus("applying");
        afterSale.setRefundNo(generateRefundNo(order));
        this.save(afterSale);

        orderGoods.setRefundStatus("applying");
        orderGoodsService.updateById(orderGoods);

        return afterSale;
    }

    @Override
    public void approve(Long afterSaleId, String auditRemark) {
        // 先按租户校验归属（越权时抛 403），再做状态条件更新——不能让"不属于当前租户"和"状态不对"
        // 共用同一个 0 行判定，否则跨租户访问会被误判为"状态不允许"（40001）而不是越权（403），
        // 与文档三 §2.4"越权异常必须映射为403"的硬性要求相悖。reject/confirmReturnShipped 同理。
        this.getByIdWithTenant(afterSaleId);
        boolean updated = this.update(Wrappers.<AfterSale>lambdaUpdate()
                .eq(AfterSale::getId, afterSaleId)
                .eq(AfterSale::getStatus, "applying")
                .set(AfterSale::getStatus, "approved")
                .set(AfterSale::getAuditRemark, auditRemark));
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "售后单当前状态不允许审核同意");
        }
    }

    @Override
    public void reject(Long afterSaleId, String auditRemark) {
        AfterSale afterSale = this.getByIdWithTenant(afterSaleId);
        boolean updated = this.update(Wrappers.<AfterSale>lambdaUpdate()
                .eq(AfterSale::getId, afterSaleId)
                .eq(AfterSale::getStatus, "applying")
                .set(AfterSale::getStatus, "rejected")
                .set(AfterSale::getAuditRemark, auditRemark));
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "售后单当前状态不允许拒绝");
        }
        resetOrderGoodsRefundStatus(afterSale.getOrderGoodsId());
    }

    @Override
    public void confirmReturnShipped(Long afterSaleId, String expressCompany, String expressNo) {
        this.getByIdWithTenant(afterSaleId);
        boolean updated = this.update(Wrappers.<AfterSale>lambdaUpdate()
                .eq(AfterSale::getId, afterSaleId)
                .eq(AfterSale::getStatus, "approved")
                .set(AfterSale::getStatus, "return_shipped")
                .set(AfterSale::getReturnExpressCompany, expressCompany)
                .set(AfterSale::getReturnExpressNo, expressNo));
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "售后单当前状态不允许提交退货物流");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeRefund(Long afterSaleId) {
        AfterSale afterSale = this.getByIdWithTenant(afterSaleId);
        String expectedPriorStatus = "return_refund".equals(afterSale.getType()) ? "return_shipped" : "approved";
        boolean movedToRefunding = this.update(Wrappers.<AfterSale>lambdaUpdate()
                .eq(AfterSale::getId, afterSaleId)
                .eq(AfterSale::getStatus, expectedPriorStatus)
                .set(AfterSale::getStatus, "refunding"));
        if (!movedToRefunding) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "售后单当前状态不允许执行退款");
        }

        Order order = orderService.getByIdWithTenant(afterSale.getOrderId());
        ChannelRefundResult channelRefund;
        try {
            channelRefund = executeChannelRefund(order, afterSale);
        } catch (Exception e) {
            logFailureIndependently(afterSale, e.getMessage());
            throw e;
        }

        RefundLog refundLog = new RefundLog();
        refundLog.setAfterSaleId(afterSaleId);
        refundLog.setRefundNo(afterSale.getRefundNo());
        refundLog.setAmount(afterSale.getRefundAmount());
        refundLog.setStatus(channelRefund.status());
        refundLog.setRawResponse(channelRefund.rawResponse());
        refundLogService.save(refundLog);

        this.update(Wrappers.<AfterSale>lambdaUpdate()
                .eq(AfterSale::getId, afterSaleId)
                .set(AfterSale::getStatus, "refunded")
                .set(AfterSale::getWxRefundId, channelRefund.externalId())
                .set(AfterSale::getRefundTime, LocalDateTime.now()));

        OrderGoods orderGoods = orderGoodsService.getByIdWithTenant(afterSale.getOrderGoodsId());
        orderGoods.setRefundStatus("refunded");
        orderGoodsService.updateById(orderGoods);
        goodsSkuService.restoreStock(orderGoods.getSkuId(), afterSale.getRefundNum());
    }

    private ChannelRefundResult executeChannelRefund(Order order, AfterSale afterSale) {
        if (ALIPAY.equals(order.getPayMethod())) {
            var config = shopPayConfigService.findDecryptedAlipayConfig()
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "商户尚未启用支付宝"));
            AlipayGateway.RefundResult refund = alipayGateway.createRefund(
                    config, order.getOrderNo(), afterSale.getRefundNo(),
                    afterSale.getRefundAmount(), afterSale.getApplyReason());
            return new ChannelRefundResult("success", refund.transactionId(), refund.rawResponse());
        }
        DecryptedPayConfig config = shopPayConfigService.findDecryptedConfig(WECHAT)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAY_CHANNEL_ERROR, "商户尚未配置微信支付"));
        Refund refund = wxPayGateway.createRefund(config, order.getOrderNo(), afterSale.getRefundNo(),
                yuanToFen(afterSale.getRefundAmount()), yuanToFen(order.getPayPrice()), afterSale.getApplyReason());
        return new ChannelRefundResult(mapRefundStatus(refund.getStatus()), refund.getRefundId(), refund.toString());
    }

    /**
     * 退款失败时，售后单状态会随外层事务回滚回 approved/return_shipped，但失败流水必须留痕方便排查——
     * 用独立事务写入，不受外层事务回滚影响。见 {@link RefundLogService#saveIndependently}。
     */
    private void logFailureIndependently(AfterSale afterSale, String errorMessage) {
        RefundLog refundLog = new RefundLog();
        refundLog.setAfterSaleId(afterSale.getId());
        refundLog.setRefundNo(afterSale.getRefundNo());
        refundLog.setAmount(afterSale.getRefundAmount());
        refundLog.setStatus("failed");
        refundLog.setRawResponse(errorMessage);
        refundLogService.saveIndependently(refundLog);
        log.error("售后退款执行失败 afterSaleId={} reason={}", afterSale.getId(), errorMessage);
    }

    @Override
    public void close(Long afterSaleId) {
        AfterSale afterSale = this.getByIdWithTenant(afterSaleId);
        if (List.of("refunded", "closed").contains(afterSale.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "售后单已是终态，不能关闭");
        }
        this.update(Wrappers.<AfterSale>lambdaUpdate()
                .eq(AfterSale::getId, afterSaleId)
                .set(AfterSale::getStatus, "closed"));
        resetOrderGoodsRefundStatus(afterSale.getOrderGoodsId());
    }

    @Override
    public List<AfterSale> listByOrderId(Long orderId) {
        return this.list(Wrappers.<AfterSale>lambdaQuery().eq(AfterSale::getOrderId, orderId));
    }

    /** 拒绝/关闭售后单后，该商品行恢复"可再次申请售后"，避免因一次被拒就永久锁死这条商品行。 */
    private void resetOrderGoodsRefundStatus(Long orderGoodsId) {
        OrderGoods orderGoods = orderGoodsService.getByIdWithTenant(orderGoodsId);
        orderGoods.setRefundStatus("none");
        orderGoodsService.updateById(orderGoods);
    }

    private String generateRefundNo(Order order) {
        String datePart = LocalDateTime.now().format(DATE_FMT);
        String randomPart = String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
        return "RF" + datePart + order.getOrderNo().substring(8, 12) + randomPart;
    }

    private long yuanToFen(BigDecimal yuan) {
        return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String mapRefundStatus(com.wechat.pay.java.service.refund.model.Status status) {
        if (status == null) {
            return "processing";
        }
        return status.name().toLowerCase();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化失败");
        }
    }

    private record ChannelRefundResult(String status, String externalId, String rawResponse) {
    }
}
