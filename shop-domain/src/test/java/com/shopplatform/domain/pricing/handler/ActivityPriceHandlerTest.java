package com.shopplatform.domain.pricing.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.entity.SeckillTime;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import com.shopplatform.domain.marketing.service.SeckillTimeService;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.BargainActiveService;
import com.shopplatform.domain.marketing.service.BargainRecordService;
import com.shopplatform.domain.pricing.OrderPriceResult;
import com.shopplatform.domain.pricing.PriceCalculator;
import com.shopplatform.domain.pricing.PriceContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ActivityPriceHandler} 单测：秒杀换价、限时折扣、未在抢购时段、售罄不换价、activityType=none 不生效。
 */
class ActivityPriceHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PriceContext.PriceItem item(long skuId, BigDecimal price, int qty) {
        return new PriceContext.PriceItem(skuId * 100, skuId, "商品" + skuId, "", "", price, null, qty, null);
    }

    private PriceContext seckillCtx(List<PriceContext.PriceItem> items, Long activeId) {
        return new PriceContext(1L, 100L, items, "express", null, null, null, "seckill", activeId);
    }

    private PriceContext noneCtx(List<PriceContext.PriceItem> items) {
        return new PriceContext(1L, 100L, items, "express", null, null, null, "none", null);
    }

    private SeckillActive active(String timeIdsJson) {
        SeckillActive a = new SeckillActive();
        a.setId(1L);
        a.setStatus("on");
        a.setStartDate(LocalDate.now());
        a.setEndDate(LocalDate.now());
        a.setTimeIds(timeIdsJson);
        return a;
    }

    private SeckillGoods sg(long skuId, BigDecimal seckillPrice, int num, int sold) {
        SeckillGoods g = new SeckillGoods();
        g.setId(1L);
        g.setActiveId(1L);
        g.setSkuId(skuId);
        g.setSeckillPrice(seckillPrice);
        g.setSeckillNum(num);
        g.setSold(sold);
        g.setStatus("on");
        return g;
    }

    @Test
    void limitedTimeDiscount_replacesPrice() {
        SeckillActiveService activeService = mock(SeckillActiveService.class);
        SeckillGoodsService goodsService = mock(SeckillGoodsService.class);
        SeckillTimeService timeService = mock(SeckillTimeService.class);
        when(activeService.getByIdWithTenant(1L)).thenReturn(active(null)); // time_ids 空 = 限时折扣
        when(goodsService.findByActiveAndSku(1L, 11L)).thenReturn(sg(11L, new BigDecimal("80.00"), 0, 0));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(activeService, goodsService, timeService,
                        mock(GroupActiveService.class), mock(BargainActiveService.class),
                        mock(BargainRecordService.class), objectMapper),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(
                seckillCtx(List.of(item(11, new BigDecimal("100.00"), 2)), 1L));

        // 原价 200，秒杀价 80×2=160，优惠 40
        assertEquals(new BigDecimal("200.00"), result.totalPrice());
        assertEquals(new BigDecimal("40.00"), result.discountPrice());
        assertEquals(new BigDecimal("160.00"), result.payPrice());
        assertEquals(new BigDecimal("40.00"), result.items().get(0).discountDetail().get("activity"));
    }

    @Test
    void seckill_inSlot_replacesPrice() {
        SeckillActiveService activeService = mock(SeckillActiveService.class);
        SeckillGoodsService goodsService = mock(SeckillGoodsService.class);
        SeckillTimeService timeService = mock(SeckillTimeService.class);
        when(activeService.getByIdWithTenant(1L)).thenReturn(active("[10]"));
        SeckillTime slot = new SeckillTime();
        slot.setId(10L);
        slot.setStatus("on");
        slot.setStartTime(LocalTime.MIN);
        slot.setEndTime(LocalTime.MAX);
        when(timeService.listByIds(List.of(10L))).thenReturn(List.of(slot));
        when(goodsService.findByActiveAndSku(1L, 11L)).thenReturn(sg(11L, new BigDecimal("80.00"), 100, 0));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(activeService, goodsService, timeService,
                        mock(GroupActiveService.class), mock(BargainActiveService.class),
                        mock(BargainRecordService.class), objectMapper),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(
                seckillCtx(List.of(item(11, new BigDecimal("100.00"), 1)), 1L));

        assertEquals(new BigDecimal("20.00"), result.discountPrice());
        assertEquals(new BigDecimal("80.00"), result.payPrice());
    }

    @Test
    void seckill_outOfSlot_throws() {
        SeckillActiveService activeService = mock(SeckillActiveService.class);
        SeckillGoodsService goodsService = mock(SeckillGoodsService.class);
        SeckillTimeService timeService = mock(SeckillTimeService.class);
        when(activeService.getByIdWithTenant(1L)).thenReturn(active("[10]"));
        SeckillTime slot = new SeckillTime();
        slot.setId(10L);
        slot.setStatus("on");
        slot.setStartTime(LocalTime.now().plusHours(1));
        slot.setEndTime(LocalTime.now().plusHours(2));
        when(timeService.listByIds(List.of(10L))).thenReturn(List.of(slot));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(activeService, goodsService, timeService,
                        mock(GroupActiveService.class), mock(BargainActiveService.class),
                        mock(BargainRecordService.class), objectMapper),
                new RoundingHandler()));

        assertThrows(BusinessException.class,
                () -> calculator.calculate(seckillCtx(List.of(item(11, new BigDecimal("100.00"), 1)), 1L)));
    }

    @Test
    void soldOut_doesNotReplacePrice() {
        SeckillActiveService activeService = mock(SeckillActiveService.class);
        SeckillGoodsService goodsService = mock(SeckillGoodsService.class);
        SeckillTimeService timeService = mock(SeckillTimeService.class);
        when(activeService.getByIdWithTenant(1L)).thenReturn(active(null));
        when(goodsService.findByActiveAndSku(1L, 11L)).thenReturn(sg(11L, new BigDecimal("80.00"), 100, 100)); // 售罄

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(activeService, goodsService, timeService,
                        mock(GroupActiveService.class), mock(BargainActiveService.class),
                        mock(BargainRecordService.class), objectMapper),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(
                seckillCtx(List.of(item(11, new BigDecimal("100.00"), 1)), 1L));

        // 售罄 → 不换价，按原价
        assertEquals(BigDecimal.ZERO, result.discountPrice());
        assertEquals(new BigDecimal("100.00"), result.payPrice());
    }

    @Test
    void activityTypeNone_handlerSkipped() {
        SeckillActiveService activeService = mock(SeckillActiveService.class);
        SeckillGoodsService goodsService = mock(SeckillGoodsService.class);
        SeckillTimeService timeService = mock(SeckillTimeService.class);

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(activeService, goodsService, timeService,
                        mock(GroupActiveService.class), mock(BargainActiveService.class),
                        mock(BargainRecordService.class), objectMapper),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(noneCtx(List.of(item(11, new BigDecimal("100.00"), 1))));

        assertEquals(BigDecimal.ZERO, result.discountPrice());
        assertEquals(new BigDecimal("100.00"), result.payPrice());
    }

    /** 秒杀换价后，满减门槛按"活动后小计"判定：原价 200 → 秒杀 160 → 满 150 减 20。 */
    @Test
    void activityThenFullReduce_thresholdUsesActivitySubtotal() {
        SeckillActiveService activeService = mock(SeckillActiveService.class);
        SeckillGoodsService goodsService = mock(SeckillGoodsService.class);
        SeckillTimeService timeService = mock(SeckillTimeService.class);
        when(activeService.getByIdWithTenant(1L)).thenReturn(active(null));
        when(goodsService.findByActiveAndSku(1L, 11L)).thenReturn(sg(11L, new BigDecimal("80.00"), 0, 0));

        com.shopplatform.domain.marketing.service.FullReduceRuleService ruleService =
                mock(com.shopplatform.domain.marketing.service.FullReduceRuleService.class);
        com.shopplatform.domain.marketing.entity.FullReduceRule rule =
                new com.shopplatform.domain.marketing.entity.FullReduceRule();
        rule.setId(1L);
        rule.setType("money");
        rule.setRules("[{\"threshold\":150,\"reduce\":20}]");
        rule.setFreeExpress(0);
        rule.setStatus("on");
        when(ruleService.listActive()).thenReturn(List.of(rule));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(activeService, goodsService, timeService,
                        mock(GroupActiveService.class), mock(BargainActiveService.class),
                        mock(BargainRecordService.class), objectMapper),
                new com.shopplatform.domain.pricing.handler.FullReduceHandler(ruleService, objectMapper),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(
                seckillCtx(List.of(item(11, new BigDecimal("100.00"), 2)), 1L));

        // 原价 200；秒杀价 80×2=160（activity 优惠 40）；满 150 减 20（fullReduce 优惠 20）
        assertEquals(new BigDecimal("200.00"), result.totalPrice());
        assertEquals(new BigDecimal("60.00"), result.discountPrice()); // activity 40 + fullReduce 20
        assertEquals(new BigDecimal("140.00"), result.payPrice());
    }

    // ---- 拼团 ----

    private PriceContext groupCtx(List<PriceContext.PriceItem> items, Long activeId) {
        return new PriceContext(1L, 100L, items, "express", null, null, null, "group", activeId);
    }

    private com.shopplatform.domain.marketing.entity.GroupActive groupActive(long goodsId, String priceJson) {
        com.shopplatform.domain.marketing.entity.GroupActive a =
                new com.shopplatform.domain.marketing.entity.GroupActive();
        a.setId(1L);
        a.setGoodsId(goodsId);
        a.setGroupPrice(priceJson);
        a.setStatus("on");
        a.setStartTime(LocalDateTime.now().minusHours(1));
        a.setEndTime(LocalDateTime.now().plusHours(1));
        return a;
    }

    @Test
    void group_replacesPriceBySku() {
        SeckillActiveService as = mock(SeckillActiveService.class);
        SeckillGoodsService gs = mock(SeckillGoodsService.class);
        SeckillTimeService ts = mock(SeckillTimeService.class);
        GroupActiveService groupService = mock(GroupActiveService.class);
        when(groupService.getByIdWithTenant(1L)).thenReturn(groupActive(1100L, "{\"11\":80.00}"));
        when(groupService.parseGroupPrice(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Map.of(11L, new BigDecimal("80.00")));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(as, gs, ts, groupService,
                        mock(BargainActiveService.class), mock(BargainRecordService.class), objectMapper),
                new RoundingHandler()));

        // item(11) 的 goodsId = 11*100 = 1100，匹配拼团活动
        OrderPriceResult result = calculator.calculate(
                groupCtx(List.of(item(11, new BigDecimal("100.00"), 2)), 1L));

        // 原价 200，拼团价 80×2=160，优惠 40
        assertEquals(new BigDecimal("200.00"), result.totalPrice());
        assertEquals(new BigDecimal("40.00"), result.discountPrice());
        assertEquals(new BigDecimal("160.00"), result.payPrice());
    }

    @Test
    void group_expired_throws() {
        SeckillActiveService as = mock(SeckillActiveService.class);
        SeckillGoodsService gs = mock(SeckillGoodsService.class);
        SeckillTimeService ts = mock(SeckillTimeService.class);
        GroupActiveService groupService = mock(GroupActiveService.class);
        com.shopplatform.domain.marketing.entity.GroupActive a = groupActive(1100L, "{\"11\":80.00}");
        a.setStatus("off");
        when(groupService.getByIdWithTenant(1L)).thenReturn(a);

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(as, gs, ts, groupService,
                        mock(BargainActiveService.class), mock(BargainRecordService.class), objectMapper),
                new RoundingHandler()));

        assertThrows(BusinessException.class,
                () -> calculator.calculate(groupCtx(List.of(item(11, new BigDecimal("100.00"), 1)), 1L)));
    }

    // ---- 砍价 ----

    private PriceContext bargainCtx(List<PriceContext.PriceItem> items, Long recordId) {
        return new PriceContext(1L, 100L, items, "express", null, null, null, "bargain", recordId);
    }

    @Test
    void bargain_replacesPriceWithCurrentPrice() {
        SeckillActiveService as = mock(SeckillActiveService.class);
        SeckillGoodsService gs = mock(SeckillGoodsService.class);
        SeckillTimeService ts = mock(SeckillTimeService.class);
        BargainRecordService recordService = mock(BargainRecordService.class);
        BargainActiveService bargainActiveService = mock(BargainActiveService.class);

        com.shopplatform.domain.marketing.entity.BargainRecord rec =
                new com.shopplatform.domain.marketing.entity.BargainRecord();
        rec.setId(1L);
        rec.setActiveId(2L);
        rec.setUserId(100L);
        rec.setCurrentPrice(new BigDecimal("69.00"));
        rec.setStatus("ongoing");
        rec.setExpireTime(LocalDateTime.now().plusHours(1));
        when(recordService.getByIdWithTenant(1L)).thenReturn(rec);

        com.shopplatform.domain.marketing.entity.BargainActive ba =
                new com.shopplatform.domain.marketing.entity.BargainActive();
        ba.setId(2L);
        ba.setGoodsId(1100L); // item(11) goodsId=1100
        when(bargainActiveService.getByIdWithTenant(2L)).thenReturn(ba);

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(as, gs, ts, mock(GroupActiveService.class),
                        bargainActiveService, recordService, objectMapper),
                new RoundingHandler()));

        // 原价 100，砍到 69，优惠 31
        OrderPriceResult result = calculator.calculate(
                bargainCtx(List.of(item(11, new BigDecimal("100.00"), 1)), 1L));

        assertEquals(new BigDecimal("100.00"), result.totalPrice());
        assertEquals(new BigDecimal("31.00"), result.discountPrice());
        assertEquals(new BigDecimal("69.00"), result.payPrice());
    }

    @Test
    void bargain_expiredRecord_throws() {
        SeckillActiveService as = mock(SeckillActiveService.class);
        SeckillGoodsService gs = mock(SeckillGoodsService.class);
        SeckillTimeService ts = mock(SeckillTimeService.class);
        BargainRecordService recordService = mock(BargainRecordService.class);
        com.shopplatform.domain.marketing.entity.BargainRecord rec =
                new com.shopplatform.domain.marketing.entity.BargainRecord();
        rec.setId(1L);
        rec.setActiveId(2L);
        rec.setUserId(100L);
        rec.setCurrentPrice(new BigDecimal("69.00"));
        rec.setStatus("expired");
        when(recordService.getByIdWithTenant(1L)).thenReturn(rec);

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new ActivityPriceHandler(as, gs, ts, mock(GroupActiveService.class),
                        mock(BargainActiveService.class), recordService, objectMapper),
                new RoundingHandler()));

        assertThrows(BusinessException.class,
                () -> calculator.calculate(bargainCtx(List.of(item(11, new BigDecimal("100.00"), 1)), 1L)));
    }
}
