package com.shopplatform.domain.aftersale;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.domain.order.entity.OrderGoods;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 售后退款金额计算：按 {@code order_goods.discount_detail} 记录的分摊比例逆向计算，
 * 不重新调用 {@link com.shopplatform.domain.pricing.PriceCalculator} 重算一遍——见文档三 §4：
 * "任何一处单独算价，最终都会出现'预览98元、实付102元'的投诉"，退款也是"再算一次价"的一种，同样不能绕开这条原则，
 * 但退款场景的"价格计算"就是"把下单时已经算好的分摊结果按比例逆推"，而不是重新走责任链。
 * <p>
 * 逆算公式（按件数比例，M2 前不支持按重量/自定义比例退款）：
 * {@code proportion = refundNum / totalNum}；
 * {@code refundDetail[type] = discountDetail[type] * proportion}（四舍五入到分）；
 * {@code refundAmount = goodsPrice * refundNum - sum(refundDetail.values())}。
 * <p>
 * 舍入方式与 {@code RoundingHandler} 保持一致（各笔独立四舍五入，累计误差不做全局校正），
 * M2 阶段如果要做全局误差校正，两处应该一起改，不要只改一处导致下单和退款的舍入口径又对不上。
 */
@Component
public class RefundCalculator {

    private final ObjectMapper objectMapper;

    public RefundCalculator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RefundResult calculate(OrderGoods orderGoods, int refundNum) {
        if (refundNum <= 0 || refundNum > orderGoods.getTotalNum()) {
            throw new IllegalArgumentException("退款件数必须大于0且不超过下单件数");
        }

        BigDecimal proportion = BigDecimal.valueOf(refundNum)
                .divide(BigDecimal.valueOf(orderGoods.getTotalNum()), 10, RoundingMode.HALF_UP);

        Map<String, BigDecimal> originalDetail = readDiscountDetail(orderGoods.getDiscountDetail());
        Map<String, BigDecimal> refundDetail = new LinkedHashMap<>();
        BigDecimal totalDiscountRefund = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : originalDetail.entrySet()) {
            BigDecimal refundPortion = entry.getValue().multiply(proportion).setScale(2, RoundingMode.HALF_UP);
            refundDetail.put(entry.getKey(), refundPortion);
            totalDiscountRefund = totalDiscountRefund.add(refundPortion);
        }

        BigDecimal refundAmount = orderGoods.getGoodsPrice()
                .multiply(BigDecimal.valueOf(refundNum))
                .subtract(totalDiscountRefund)
                .setScale(2, RoundingMode.HALF_UP);

        return new RefundResult(refundAmount, refundDetail);
    }

    private Map<String, BigDecimal> readDiscountDetail(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, BigDecimal>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    public record RefundResult(BigDecimal refundAmount, Map<String, BigDecimal> refundDetail) {
    }
}
