package com.shopplatform.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 优惠金额按"行实付小计占比"分摊到各购物行。见文档三 §4：
 * "每一笔优惠都要按商品实付比例分摊到 order_goods，退款时按分摊后的金额退。"
 * <p>
 * 本工具只负责把总额按权重切成各行份额（高精度，不在此处舍入到分），舍入与尾差收口
 * 统一由 {@link com.shopplatform.domain.pricing.handler.RoundingHandler} 在链路末尾做一次，
 * 避免每个 Handler 各写一套舍入逻辑、各带各的尾差 bug。
 */
public final class DiscountApportioner {

    private DiscountApportioner() {
    }

    /**
     * 把 {@code total} 按 {@code weights} 的占比分摊到 {@code items} 的 {@code discountDetail[type]}。
     * {@code items} 与 {@code weights} 等长、一一对应；权重为各行"实付小计"（currentSubtotal）。
     * 总额或权重和 ≤ 0 时直接跳过（无优惠可分摊）。
     */
    public static void apportion(List<PriceWorkingState.WorkingItem> items,
                                   List<BigDecimal> weights,
                                   BigDecimal total,
                                   String type) {
        if (items.isEmpty()) {
            return;
        }
        BigDecimal weightSum = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            if (w != null && w.signum() > 0) {
                weightSum = weightSum.add(w);
            }
        }
        if (weightSum.signum() <= 0 || total == null || total.signum() <= 0) {
            return;
        }

        BigDecimal remaining = total;
        for (int i = 0; i < items.size(); i++) {
            BigDecimal weight = weights.get(i);
            BigDecimal share;
            if (i == items.size() - 1) {
                // 最后一行吸收除法尾差，保证各行份额之和精确等于 total（再交由 RoundingHandler 做舍入收口）
                share = remaining;
            } else {
                share = total.multiply(weight == null ? BigDecimal.ZERO : weight)
                        .divide(weightSum, 10, RoundingMode.HALF_UP);
                remaining = remaining.subtract(share);
            }
            items.get(i).addDiscount(type, share);
        }
    }
}
