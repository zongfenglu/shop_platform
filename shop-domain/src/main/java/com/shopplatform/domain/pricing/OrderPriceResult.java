package com.shopplatform.domain.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 价格计算的最终输出。下单预览、正式下单、售后退款金额计算共用同一份结构——
 * 这是文档三 §4 强调的"必须共用一套代码"的落地形式：任何调用方拿到的都是这个结构，
 * 不会出现"预览页自己算了一份 total，下单接口又自己算了一份 total"的分裂实现。
 */
public record OrderPriceResult(
        BigDecimal totalPrice,
        BigDecimal discountPrice,
        BigDecimal couponPrice,
        BigDecimal pointsPrice,
        BigDecimal expressPrice,
        BigDecimal payPrice,
        List<ItemResult> items
) {

    /**
     * 单个购物项的最终结果。{@code totalPrice} 是分摊前的原价小计，
     * {@code discountDetail} 是分摊到本行的每类优惠金额——两者相减即为本行实付金额。
     * 分摊金额总和（对全部 items 求和）必须等于 discountPrice+couponPrice+pointsPrice，
     * 这是 Sprint 4 验收标准里明确要求的断言。
     */
    public record ItemResult(
            Long goodsId,
            Long skuId,
            String goodsName,
            String specText,
            String image,
            BigDecimal goodsPrice,
            BigDecimal linePrice,
            Integer totalNum,
            BigDecimal totalPrice,
            Map<String, BigDecimal> discountDetail
    ) {
    }
}
