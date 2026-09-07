package com.shopplatform.domain.pricing.handler;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.pricing.OrderPriceResult;
import com.shopplatform.domain.pricing.PriceCalculator;
import com.shopplatform.domain.pricing.PriceContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link PointsHandler} 单测：积分抵扣生效、受最高比例上限、积分不足抛异常、未用积分跳过、游客预览按比例上限。
 */
class PointsHandlerTest {

    private PriceContext.PriceItem item(long skuId, BigDecimal price, int qty) {
        return new PriceContext.PriceItem(skuId * 100, skuId, "商品" + skuId, "", "", price, null, qty, null);
    }

    private PriceContext ctx(List<PriceContext.PriceItem> items, Integer points) {
        return new PriceContext(1L, 100L, items, "express", null, null, points, "none", null);
    }

    private Member member(int points) {
        Member m = new Member();
        m.setId(100L);
        m.setPoints(points);
        return m;
    }

    private PointsHandler handler(MemberService memberService, int rate, double ratio) {
        PointsHandler h = new PointsHandler(memberService);
        ReflectionTestUtils.setField(h, "exchangeRate", rate);
        ReflectionTestUtils.setField(h, "maxRatio", BigDecimal.valueOf(ratio));
        return h;
    }

    @Test
    void pointsDeduct_applies() {
        MemberService memberService = mock(MemberService.class);
        when(memberService.getByIdWithTenant(100L)).thenReturn(member(1000));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                handler(memberService, 100, 0.5),
                new RoundingHandler()));

        // 原价 200，用 1000 积分 = 10 元，未超 50% 上限（100），实付 190
        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 2)), 1000));

        assertEquals(new BigDecimal("200.00"), result.totalPrice());
        assertEquals(new BigDecimal("10.00"), result.pointsPrice());
        assertEquals(new BigDecimal("190.00"), result.payPrice());
        assertEquals(new BigDecimal("10.00"), result.items().get(0).discountDetail().get("points"));
    }

    @Test
    void pointsDeduct_cappedByMaxRatio() {
        MemberService memberService = mock(MemberService.class);
        when(memberService.getByIdWithTenant(100L)).thenReturn(member(100000));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                handler(memberService, 100, 0.5),
                new RoundingHandler()));

        // 原价 200，50% 上限 = 100；用 50000 积分 = 500 元 → 被上限截到 100
        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 2)), 50000));

        assertEquals(new BigDecimal("100.00"), result.pointsPrice());
        assertEquals(new BigDecimal("100.00"), result.payPrice());
    }

    @Test
    void pointsInsufficient_throws() {
        MemberService memberService = mock(MemberService.class);
        when(memberService.getByIdWithTenant(100L)).thenReturn(member(100)); // 只有 100 积分

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                handler(memberService, 100, 0.5),
                new RoundingHandler()));

        assertThrows(BusinessException.class,
                () -> calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 1)), 1000)));
    }

    @Test
    void noPoints_skips() {
        MemberService memberService = mock(MemberService.class);

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                handler(memberService, 100, 0.5),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 1)), null));
        assertEquals(BigDecimal.ZERO, result.pointsPrice());
        assertEquals(new BigDecimal("100.00"), result.payPrice());
    }

    /** 会员折扣 + 积分叠加：原价 200，九折后 180，积分抵 10，实付 170。验证责任链顺序。 */
    @Test
    void memberDiscountThenPoints_chain() {
        MemberService memberService = mock(MemberService.class);
        com.shopplatform.domain.member.service.UserGradeService gradeService =
                mock(com.shopplatform.domain.member.service.UserGradeService.class);
        when(memberService.getByIdWithTenant(100L)).thenReturn(member(1000));
        com.shopplatform.domain.member.entity.UserGrade g = new com.shopplatform.domain.member.entity.UserGrade();
        g.setId(1L);
        g.setDiscountRatio(new BigDecimal("0.90"));
        when(gradeService.getByIdWithTenant(1L)).thenReturn(g);
        // member() 复用：会员折扣需 gradeId
        Member m = member(1000);
        m.setGradeId(1L);
        when(memberService.getByIdWithTenant(100L)).thenReturn(m);

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new MemberDiscountHandler(memberService, gradeService),
                handler(memberService, 100, 0.5),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 2)), 1000));

        // 原价 200；memberDiscount 20 → 180；points 10 → 实付 170
        assertEquals(new BigDecimal("20.00"), result.discountPrice());
        assertEquals(new BigDecimal("10.00"), result.pointsPrice());
        assertEquals(new BigDecimal("170.00"), result.payPrice());
    }
}
