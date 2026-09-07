package com.shopplatform.domain.pricing.handler;

import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.UserGradeService;
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
 * {@link MemberDiscountHandler} 单测：等级折扣生效、活动商品不叠加、无等级/无折扣跳过、记录缺失静默跳过。
 */
class MemberDiscountHandlerTest {

    private PriceContext.PriceItem item(long skuId, BigDecimal price, int qty) {
        return new PriceContext.PriceItem(skuId * 100, skuId, "商品" + skuId, "", "", price, null, qty, null);
    }

    private PriceContext ctx(List<PriceContext.PriceItem> items) {
        return new PriceContext(1L, 100L, items, "express", null, null, null, "none", null);
    }

    private Member member(Long gradeId) {
        Member m = new Member();
        m.setId(100L);
        m.setGradeId(gradeId);
        return m;
    }

    private UserGrade grade(BigDecimal ratio) {
        UserGrade g = new UserGrade();
        g.setId(1L);
        g.setDiscountRatio(ratio);
        return g;
    }

    @Test
    void gradeDiscount_appliesToNonActivityItems() {
        MemberService memberService = mock(MemberService.class);
        UserGradeService gradeService = mock(UserGradeService.class);
        when(memberService.getByIdWithTenant(100L)).thenReturn(member(1L));
        when(gradeService.getByIdWithTenant(1L)).thenReturn(grade(new BigDecimal("0.90"))); // 九折

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new MemberDiscountHandler(memberService, gradeService),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 2))));

        // 原价 200，九折优惠 20，实付 180
        assertEquals(new BigDecimal("200.00"), result.totalPrice());
        assertEquals(new BigDecimal("20.00"), result.discountPrice());
        assertEquals(new BigDecimal("180.00"), result.payPrice());
        assertEquals(new BigDecimal("20.00"), result.items().get(0).discountDetail().get("memberDiscount"));
    }

    @Test
    void activityItem_skipsMemberDiscount() {
        MemberService memberService = mock(MemberService.class);
        UserGradeService gradeService = mock(UserGradeService.class);
        when(memberService.getByIdWithTenant(100L)).thenReturn(member(1L));
        when(gradeService.getByIdWithTenant(1L)).thenReturn(grade(new BigDecimal("0.90")));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new MemberDiscountHandler(memberService, gradeService),
                new RoundingHandler()));

        // 手动给行加上 activity 分摊，模拟 ActivityPriceHandler 已换价
        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 1))));
        // 先正常算一次确认无 activity 时有 memberDiscount
        assertEquals(new BigDecimal("10.00"), result.discountPrice());

        // 构造一个已被 activity 换价的 state：直接验证 handler 跳过逻辑
        com.shopplatform.domain.pricing.PriceWorkingState state =
                new com.shopplatform.domain.pricing.PriceWorkingState(ctx(List.of(item(11, new BigDecimal("100.00"), 1))));
        state.items().get(0).addDiscount("activity", new BigDecimal("20.00"));
        new MemberDiscountHandler(memberService, gradeService).handle(ctx(List.of()), state);
        // 活动行不应新增 memberDiscount
        assertEquals(new BigDecimal("20.00"), state.items().get(0).discountDetail().get("activity"));
        assertEquals(null, state.items().get(0).discountDetail().get("memberDiscount"));
    }

    @Test
    void noGrade_skips() {
        MemberService memberService = mock(MemberService.class);
        UserGradeService gradeService = mock(UserGradeService.class);
        when(memberService.getByIdWithTenant(100L)).thenReturn(member(null));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new MemberDiscountHandler(memberService, gradeService),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 1))));
        assertEquals(BigDecimal.ZERO, result.discountPrice());
        assertEquals(new BigDecimal("100.00"), result.payPrice());
    }

    @Test
    void ratioOne_skips() {
        MemberService memberService = mock(MemberService.class);
        UserGradeService gradeService = mock(UserGradeService.class);
        when(memberService.getByIdWithTenant(100L)).thenReturn(member(1L));
        when(gradeService.getByIdWithTenant(1L)).thenReturn(grade(new BigDecimal("1.00"))); // 无折扣

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new MemberDiscountHandler(memberService, gradeService),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 1))));
        assertEquals(BigDecimal.ZERO, result.discountPrice());
        assertEquals(new BigDecimal("100.00"), result.payPrice());
    }

    @Test
    void memberNotFound_skipsSilently() {
        MemberService memberService = mock(MemberService.class);
        UserGradeService gradeService = mock(UserGradeService.class);
        when(memberService.getByIdWithTenant(100L)).thenThrow(new TenantAccessDeniedException("user=100"));

        PriceCalculator calculator = new PriceCalculator(List.of(
                new BasePriceHandler(),
                new MemberDiscountHandler(memberService, gradeService),
                new RoundingHandler()));

        OrderPriceResult result = calculator.calculate(ctx(List.of(item(11, new BigDecimal("100.00"), 1))));
        // 游客/无会员 → 不报错，按原价
        assertEquals(new BigDecimal("100.00"), result.payPrice());
    }
}
