package com.shopplatform.domain.pricing.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.domain.marketing.entity.FullReduceRule;
import com.shopplatform.domain.marketing.service.FullReduceRuleService;
import com.shopplatform.domain.pricing.DiscountApportioner;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.domain.pricing.PriceHandler;
import com.shopplatform.domain.pricing.PriceHandlerType;
import com.shopplatform.domain.pricing.PriceWorkingState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 责任链第4节点：满减 / 满件折。见文档三 §4。
 * <p>
 * 取当前租户下排序最高的一条生效规则（多数店铺只配一条；多规则时按 sort 取主规则，避免叠加双扣）：
 * <ul>
 *   <li>type=money：门槛按"参与商品实付小计"（currentGoodsTotal，即活动价/会员折扣后的当前小计）判定，
 *       取满足门槛中最大的一档 reduce 金额；</li>
 *   <li>type=count：按总件数判定，取满足门槛中最大的一档，按 discount 折扣，优惠=小计×(1-discount)。</li>
 * </ul>
 * 命中后把优惠总额按各行 currentSubtotal 占比分摊到 discountDetail["fullReduce"]；
 * 若规则 free_express=1 则置 freeExpress，由 {@link FreightHandler} 免运费。
 * <p>
 * 优惠金额不会超过商品小计（避免出现负实付），分摊与舍入交给 {@link DiscountApportioner} / {@link RoundingHandler}。
 */
@Component
public class FullReduceHandler implements PriceHandler {

    private final FullReduceRuleService fullReduceRuleService;
    private final ObjectMapper objectMapper;

    public FullReduceHandler(FullReduceRuleService fullReduceRuleService, ObjectMapper objectMapper) {
        this.fullReduceRuleService = fullReduceRuleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PriceHandlerType type() {
        return PriceHandlerType.FULL_REDUCE;
    }

    @Override
    public void handle(PriceContext ctx, PriceWorkingState state) {
        List<FullReduceRule> rules = fullReduceRuleService.listActive();
        if (rules.isEmpty()) {
            return;
        }
        FullReduceRule rule = rules.get(0);

        BigDecimal goodsTotal = state.currentGoodsTotal();
        int totalQty = 0;
        for (PriceWorkingState.WorkingItem item : state.items()) {
            totalQty += item.source().quantity();
        }

        BigDecimal reduceAmount = computeReduce(rule, goodsTotal, totalQty);
        if (reduceAmount == null || reduceAmount.signum() <= 0) {
            return;
        }
        // 优惠不得超过商品小计，避免出现负实付
        if (reduceAmount.compareTo(goodsTotal) > 0) {
            reduceAmount = goodsTotal;
        }

        List<BigDecimal> weights = new ArrayList<>();
        for (PriceWorkingState.WorkingItem item : state.items()) {
            weights.add(item.currentSubtotal());
        }
        DiscountApportioner.apportion(state.items(), weights, reduceAmount, "fullReduce");

        if (rule.getFreeExpress() != null && rule.getFreeExpress() == 1) {
            state.setFreeExpress(true);
        }
    }

    /** 返回命中档位的优惠金额；未命中门槛返回 null。 */
    private BigDecimal computeReduce(FullReduceRule rule, BigDecimal goodsTotal, int totalQty) {
        JsonNode tiers;
        try {
            tiers = objectMapper.readTree(rule.getRules());
            if (!tiers.isArray() || tiers.isEmpty()) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        BigDecimal measure = "count".equals(rule.getType())
                ? BigDecimal.valueOf(totalQty)
                : goodsTotal;

        JsonNode hit = null;
        for (JsonNode tier : tiers) {
            BigDecimal threshold = new BigDecimal(tier.path("threshold").asText("0"));
            if (measure.compareTo(threshold) >= 0) {
                if (hit == null || threshold.compareTo(new BigDecimal(hit.path("threshold").asText("0"))) > 0) {
                    hit = tier;
                }
            }
        }
        if (hit == null) {
            return null;
        }

        if ("count".equals(rule.getType())) {
            BigDecimal discount = new BigDecimal(hit.path("discount").asText("1"));
            return goodsTotal.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(hit.path("reduce").asText("0")).setScale(2, RoundingMode.HALF_UP);
    }
}
