package com.shopplatform.domain.pricing.handler;

import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.domain.pricing.PriceHandler;
import com.shopplatform.domain.pricing.PriceHandlerType;
import com.shopplatform.domain.pricing.PriceWorkingState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 责任链第8节点（最后一个）：分摊与舍入。见文档三 §4：
 * "优惠分摊是必须做的（不做的话部分退款一定算错）：每一笔优惠都要按商品实付比例分摊到 order_goods，
 * 退款时按分摊后的金额退。"
 * <p>
 * M2 落地 ActivityPriceHandler/FullReduceHandler/CouponHandler/PointsHandler 后，约定的写法是：
 * 每个 Handler 算出"本次优惠总额"后，按各购物项"实付小计占总实付小计的比例"把总额分摊到每一行
 * 的 {@code discountDetail}（高精度、不各自舍入）。本 Handler 统一在链路末尾对所有已写入的优惠类型
 * 做一次"逐行金额四舍五入到分，尾差补到金额最大的一行"的收口处理，保证：
 * <pre>sum(各行该类型分摊金额) 精确等于该类型优惠总额</pre>
 * 而不是让每个 Handler 各自实现一套分摊+舍入逻辑，出现"优惠券的分摊代码"和"满减的分摊代码"
 * 各写一遍、各有各的舍入 bug 的情况。
 * <p>
 * 当没有任何 Handler 写入 discountDetail（如只下单无任何营销活动）时，本 Handler 实际不做任何事。
 */
@Component
public class RoundingHandler implements PriceHandler {

    @Override
    public PriceHandlerType type() {
        return PriceHandlerType.ROUNDING;
    }

    @Override
    public void handle(PriceContext ctx, PriceWorkingState state) {
        List<PriceWorkingState.WorkingItem> items = state.items();
        if (items.isEmpty()) {
            return;
        }

        Set<String> discountTypes = new LinkedHashSet<>();
        for (PriceWorkingState.WorkingItem item : items) {
            discountTypes.addAll(item.discountDetail().keySet());
        }
        for (String type : discountTypes) {
            roundAndFixTailDiff(items, type);
        }
    }

    /**
     * 把某类优惠在各行的分摊金额舍入到分，并把舍入尾差补到金额最大的一行，保证
     * {@code sum(各行该类型分摊) 精确等于 该类型优惠总额（按原始份额求和后四舍五入到分）}。
     * <p>
     * 这是 Sprint 8 验收"分摊金额总和=实付金额、不出现四舍五入误差累积"的落地：
     * 各行独立四舍五入后总和可能与目标差 ±0.01~0.0N，统一在此处补到最大行，
     * 避免每个 Handler 各写一套尾差逻辑。只处理实际写入了该类型分摊的行（满减分摊到全部行，
     * 优惠券只分摊到参与行，未参与的行不写入 0 以免污染 order_goods.discount_detail）。
     */
    private void roundAndFixTailDiff(List<PriceWorkingState.WorkingItem> items, String discountType) {
        List<PriceWorkingState.WorkingItem> participants = new ArrayList<>();
        BigDecimal rawSum = BigDecimal.ZERO;
        for (PriceWorkingState.WorkingItem item : items) {
            BigDecimal raw = item.discountDetail().get(discountType);
            if (raw == null) {
                continue;
            }
            participants.add(item);
            rawSum = rawSum.add(raw);
        }
        if (participants.isEmpty()) {
            return;
        }

        BigDecimal intendedTotal = rawSum.setScale(2, RoundingMode.HALF_UP);

        PriceWorkingState.WorkingItem maxItem = null;
        BigDecimal maxAmount = null;
        BigDecimal roundedSum = BigDecimal.ZERO;
        for (PriceWorkingState.WorkingItem item : participants) {
            BigDecimal rounded = item.discountDetail().get(discountType).setScale(2, RoundingMode.HALF_UP);
            item.discountDetail().put(discountType, rounded);
            roundedSum = roundedSum.add(rounded);
            if (maxAmount == null || rounded.compareTo(maxAmount) > 0) {
                maxAmount = rounded;
                maxItem = item;
            }
        }

        BigDecimal diff = intendedTotal.subtract(roundedSum);
        if (diff.signum() != 0 && maxItem != null) {
            maxItem.discountDetail().put(discountType, maxAmount.add(diff));
        }
    }
}
