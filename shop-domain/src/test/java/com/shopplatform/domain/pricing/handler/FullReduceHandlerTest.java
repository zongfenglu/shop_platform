package com.shopplatform.domain.pricing.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.domain.marketing.entity.FullReduceRule;
import com.shopplatform.domain.marketing.service.FullReduceRuleService;
import com.shopplatform.domain.order.entity.FreightTemplate;
import com.shopplatform.domain.order.service.FreightTemplateService;
import com.shopplatform.domain.pricing.OrderPriceResult;
import com.shopplatform.domain.pricing.PriceCalculator;
import com.shopplatform.domain.pricing.PriceContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link FullReduceHandler} 单测：满金额减、满件折、门槛未命中、分摊尾差收口、满X包邮。
 * 验收点见 Sprint 8：分摊金额总和=优惠总额、不出现四舍五入误差累积。
 */
class FullReduceHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PriceContext ctx(List<PriceContext.PriceItem> items) {
        return new PriceContext(1L, 100L, items, "express", null, null, null, "none", null);
    }

    private PriceContext.PriceItem item(long id, BigDecimal price, int qty) {
        return new PriceContext.PriceItem(id, id * 10, "商品" + id, "", "", price, null, qty, null);
    }

    private FullReduceRule rule(String type, String rulesJson, int freeExpress) {
        FullReduceRule r = new FullReduceRule();
        r.setId(1L);
        r.setType(type);
        r.setRules(rulesJson);
        r.setFreeExpress(freeExpress);
        r.setStatus("on");
        return r;
    }

    @Test
    void moneyType_hitsHighestTier_apportionsWithTailDiffFix() {
        FullReduceRuleService ruleService = mock(FullReduceRuleService.class);
        when(ruleService.listActive()).thenReturn(List.of(
                rule("money", "[{\"threshold\":100,\"reduce\":10},{\"threshold\":200,\"reduce\":25}]", 0)));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(), new FullReduceHandler(ruleService, objectMapper), new RoundingHandler()));

        // 3 行各 100，小计 300，命中 200 减 25 档
        OrderPriceResult result = calculator.calculate(ctx(List.of(item(1, new BigDecimal("100.00"), 1),
                item(2, new BigDecimal("100.00"), 1), item(3, new BigDecimal("100.00"), 1))));

        assertEquals(new BigDecimal("300.00"), result.totalPrice());
        assertEquals(new BigDecimal("25.00"), result.discountPrice());
        // 25 分摊到 3 行：8.34 / 8.33 / 8.33，尾差补到最大行，总和精确等于 25.00
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderPriceResult.ItemResult i : result.items()) {
            sum = sum.add(i.discountDetail().get("fullReduce"));
        }
        assertEquals(new BigDecimal("25.00"), sum);
        assertEquals(new BigDecimal("275.00"), result.payPrice());
    }

    @Test
    void countType_fullPiecesDiscount_apportionsEvenly() {
        FullReduceRuleService ruleService = mock(FullReduceRuleService.class);
        when(ruleService.listActive()).thenReturn(List.of(
                rule("count", "[{\"threshold\":2,\"discount\":0.90}]", 0)));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(), new FullReduceHandler(ruleService, objectMapper), new RoundingHandler()));

        // 3 件，小计 300，满2件9折 → 优惠 30
        OrderPriceResult result = calculator.calculate(ctx(List.of(item(1, new BigDecimal("100"), 1),
                item(2, new BigDecimal("100"), 1), item(3, new BigDecimal("100"), 1))));

        assertEquals(new BigDecimal("30.00"), result.discountPrice());
        assertEquals(new BigDecimal("270.00"), result.payPrice());
    }

    @Test
    void thresholdNotMet_noDiscount() {
        FullReduceRuleService ruleService = mock(FullReduceRuleService.class);
        when(ruleService.listActive()).thenReturn(List.of(
                rule("money", "[{\"threshold\":500,\"reduce\":50}]", 0)));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(), new FullReduceHandler(ruleService, objectMapper), new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(ctx(List.of(item(1, new BigDecimal("100"), 1))));

        assertEquals(BigDecimal.ZERO, result.discountPrice());
        assertEquals(new BigDecimal("100.00"), result.payPrice());
    }

    @Test
    void noActiveRule_noDiscount() {
        FullReduceRuleService ruleService = mock(FullReduceRuleService.class);
        when(ruleService.listActive()).thenReturn(List.of());

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(), new FullReduceHandler(ruleService, objectMapper), new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(ctx(List.of(item(1, new BigDecimal("100"), 1))));

        assertEquals(BigDecimal.ZERO, result.discountPrice());
    }

    @Test
    void freeExpress_triggeredByFullReduce_zeroesFreight() {
        FullReduceRuleService ruleService = mock(FullReduceRuleService.class);
        when(ruleService.listActive()).thenReturn(List.of(
                rule("money", "[{\"threshold\":100,\"reduce\":10}]", 1)));

        FreightTemplateService freightTemplateService = mock(FreightTemplateService.class);
        FreightTemplate template = new FreightTemplate();
        template.setMethod("count");
        template.setRules("[{\"first\":1,\"firstFee\":\"10\",\"additional\":1,\"additionalFee\":\"5\"}]");
        template.setFreeRules(null);
        when(freightTemplateService.getByIdWithTenant(99L)).thenReturn(template);

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new FullReduceHandler(ruleService, objectMapper),
                new FreightHandler(freightTemplateService, objectMapper),
                new RoundingHandler()));

        // 满减命中且 freeExpress=1 → 运费应为 0
        OrderPriceResult result = calculator.calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1)),
                        "express", 99L, null, null, "none", null));

        assertEquals(BigDecimal.ZERO, result.expressPrice());
        assertEquals(new BigDecimal("90.00"), result.payPrice());
    }

    @Test
    void freeExpressNotTriggered_freightChargedNormally() {
        FullReduceRuleService ruleService = mock(FullReduceRuleService.class);
        when(ruleService.listActive()).thenReturn(List.of(
                rule("money", "[{\"threshold\":1000,\"reduce\":10}]", 1))); // 门槛未命中

        FreightTemplateService freightTemplateService = mock(FreightTemplateService.class);
        FreightTemplate template = new FreightTemplate();
        template.setMethod("count");
        template.setRules("[{\"first\":1,\"firstFee\":\"10\",\"additional\":1,\"additionalFee\":\"5\"}]");
        template.setFreeRules(null);
        when(freightTemplateService.getByIdWithTenant(99L)).thenReturn(template);

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new FullReduceHandler(ruleService, objectMapper),
                new FreightHandler(freightTemplateService, objectMapper),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1)),
                        "express", 99L, null, null, "none", null));

        // 门槛未命中 → 不包邮，运费 10
        assertEquals(new BigDecimal("10.00"), result.expressPrice());
        assertEquals(new BigDecimal("110.00"), result.payPrice());
    }
}
