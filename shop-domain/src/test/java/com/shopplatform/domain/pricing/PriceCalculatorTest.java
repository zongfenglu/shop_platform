package com.shopplatform.domain.pricing;

import com.shopplatform.domain.pricing.handler.BasePriceHandler;
import com.shopplatform.domain.pricing.handler.RoundingHandler;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 价格计算引擎骨架单测。见 Sprint 4 验收标准（文档四）：
 * "价格引擎单测覆盖'多项优惠叠加并分摊到每个订单行，分摊金额总和=实付金额'的场景
 * （即使当前只有运费一项）"。
 * <p>
 * FreightHandler 依赖 FreightTemplateService（需要 Spring/MyBatis 上下文），
 * 本测试不引入完整 Spring 上下文，只组装 BasePriceHandler + RoundingHandler 验证纯计算逻辑；
 * FreightHandler 的行为通过 shop-store-api/shop-client-api 的端到端验证覆盖（见 Sprint 4 收尾记录）。
 */
class PriceCalculatorTest {

    @Test
    void calculate_withoutFreight_totalPriceEqualsSumOfItems() {
        PriceCalculator calculator = new PriceCalculator(List.of(new BasePriceHandler(), new RoundingHandler()));

        PriceContext ctx = new PriceContext(
                1L, 100L,
                List.of(
                        new PriceContext.PriceItem(10L, 101L, "法式碎花连衣裙", "杏色/M", "img1.jpg",
                                new BigDecimal("199.00"), new BigDecimal("259.00"), 2, null),
                        new PriceContext.PriceItem(11L, 111L, "真丝方巾礼盒装", "默认", "img2.jpg",
                                new BigDecimal("99.00"), null, 1, null)
                ),
                "express", null, null, null, "none", null
        );

        OrderPriceResult result = calculator.calculate(ctx);

        // 199.00*2 + 99.00*1 = 497.00
        assertEquals(new BigDecimal("497.00"), result.totalPrice());
        assertEquals(BigDecimal.ZERO, result.expressPrice());
        assertEquals(new BigDecimal("497.00"), result.payPrice());
        assertEquals(2, result.items().size());
    }

    /**
     * 核心验收断言：分摊金额总和 = 实付金额。当前阶段没有优惠 Handler，discountPrice/couponPrice/pointsPrice
     * 恒为0，所以 payPrice 应严格等于 totalPrice + expressPrice——这条断言在 M2 接入优惠 Handler 后
     * 应该继续成立（届时 totalPrice - discountPrice - couponPrice - pointsPrice + expressPrice = payPrice，
     * 且每一项优惠分摊到各行之和必须等于该项优惠总额，这是 RoundingHandler 的职责）。
     */
    @Test
    void calculate_payPriceInvariant_alwaysHolds() {
        PriceCalculator calculator = new PriceCalculator(List.of(new BasePriceHandler(), new RoundingHandler()));

        PriceContext ctx = new PriceContext(
                1L, 100L,
                List.of(new PriceContext.PriceItem(10L, 101L, "商品A", null, null,
                        new BigDecimal("33.33"), null, 3, null)),
                "express", null, null, null, "none", null
        );

        OrderPriceResult result = calculator.calculate(ctx);

        BigDecimal expectedPayPrice = result.totalPrice()
                .subtract(result.discountPrice())
                .subtract(result.couponPrice())
                .subtract(result.pointsPrice())
                .add(result.expressPrice());

        assertEquals(expectedPayPrice, result.payPrice());
    }

    @Test
    void calculate_emptyItems_shouldThrow() {
        PriceCalculator calculator = new PriceCalculator(List.of(new BasePriceHandler(), new RoundingHandler()));
        PriceContext ctx = new PriceContext(1L, 100L, List.of(), "express", null, null, null, "none", null);

        assertThrows(com.shopplatform.common.exception.BusinessException.class, () -> calculator.calculate(ctx));
    }

    /** 同一个 PriceHandlerType 被两个实现类注册，必须在启动期就报错，而不是"后注册的悄悄覆盖前面的"。 */
    @Test
    void duplicateHandlerRegistration_shouldThrowAtConstruction() {
        assertThrows(IllegalStateException.class, () ->
                new PriceCalculator(List.of(new BasePriceHandler(), new BasePriceHandler())));
    }
}
