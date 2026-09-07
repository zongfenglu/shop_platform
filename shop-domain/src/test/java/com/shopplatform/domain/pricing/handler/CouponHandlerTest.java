package com.shopplatform.domain.pricing.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.marketing.entity.UserCoupon;
import com.shopplatform.domain.marketing.service.CouponSnapshot;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.pricing.OrderPriceResult;
import com.shopplatform.domain.pricing.PriceCalculator;
import com.shopplatform.domain.pricing.PriceContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CouponHandler} 单测：满减券/折扣券、适用范围(all/goods/category)、门槛校验、
 * 归属/状态/过期校验、分摊尾差收口。验收点见 Sprint 8。
 */
class CouponHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PriceContext.PriceItem item(long id, BigDecimal price, int qty, List<Long> categoryIds) {
        return new PriceContext.PriceItem(id, id * 10, "商品" + id, "", "", price, null, qty, null, null, categoryIds);
    }

    private UserCoupon userCoupon(Long id, Long userId, String status, LocalDateTime endTime, CouponSnapshot snap) {
        UserCoupon uc = new UserCoupon();
        uc.setId(id);
        uc.setUserId(userId);
        uc.setStatus(status);
        uc.setEndTime(endTime);
        try {
            uc.setSnapshot(objectMapper.writeValueAsString(snap));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return uc;
    }

    private PriceCalculator calculator(UserCouponService userCouponService) {
        return new PriceCalculator(List.of(
                new BasePriceHandler(),
                new CouponHandler(userCouponService, objectMapper),
                new RoundingHandler()));
    }

    @Test
    void reduceCoupon_allRange_meetsThreshold_apportionsAndDeducts() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("满100减10", "reduce",
                new BigDecimal("10.00"), null, new BigDecimal("100.00"), "all", null);
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 100L, "unused", null, snap));

        // 2 行各 100，小计 200，满100减10 → 优惠 10，分摊 5/5
        OrderPriceResult result = calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1, null),
                        item(2, new BigDecimal("100"), 1, null)), "express", null, 1L, null, "none", null));

        assertEquals(new BigDecimal("10.00"), result.couponPrice());
        assertEquals(new BigDecimal("190.00"), result.payPrice());
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderPriceResult.ItemResult i : result.items()) {
            sum = sum.add(i.discountDetail().get("coupon"));
        }
        assertEquals(new BigDecimal("10.00"), sum);
    }

    @Test
    void reduceCoupon_threeLines_tailDiffFixedOnLargestLine() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("满100减10", "reduce",
                new BigDecimal("10.00"), null, new BigDecimal("100.00"), "all", null);
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 100L, "unused", null, snap));

        // 3 行各 100，减 10 → 分摊 3.34/3.33/3.33，总和精确 10.00
        OrderPriceResult result = calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1, null),
                        item(2, new BigDecimal("100"), 1, null),
                        item(3, new BigDecimal("100"), 1, null)), "express", null, 1L, null, "none", null));

        BigDecimal sum = BigDecimal.ZERO;
        for (OrderPriceResult.ItemResult i : result.items()) {
            sum = sum.add(i.discountDetail().get("coupon"));
        }
        assertEquals(new BigDecimal("10.00"), sum);
        assertEquals(new BigDecimal("290.00"), result.payPrice());
    }

    @Test
    void discountCoupon_allRange_apportionsBySubtotal() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("9折券", "discount",
                null, new BigDecimal("0.90"), BigDecimal.ZERO, "all", null);
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 100L, "unused", null, snap));

        // 2 行 100+200=300，9折 → 优惠 30，按小计分摊 10/20
        OrderPriceResult result = calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1, null),
                        item(2, new BigDecimal("100"), 2, null)), "express", null, 1L, null, "none", null));

        assertEquals(new BigDecimal("30.00"), result.couponPrice());
        assertEquals(new BigDecimal("270.00"), result.payPrice());
    }

    @Test
    void goodsRange_onlyMatchingItemsParticipate() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("指定商品券", "reduce",
                new BigDecimal("10.00"), null, BigDecimal.ZERO, "goods", List.of(2L));
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 100L, "unused", null, snap));

        // 商品1 不参与，商品2 参与（小计100），减10
        OrderPriceResult result = calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1, null),
                        item(2, new BigDecimal("100"), 1, null)), "express", null, 1L, null, "none", null));

        // 优惠 10 只分摊到商品2
        assertEquals(new BigDecimal("10.00"), result.couponPrice());
        assertEquals(new BigDecimal("190.00"), result.payPrice());
        assertEquals(new BigDecimal("10.00"), result.items().get(1).discountDetail().get("coupon"));
        // 商品1 没有 coupon 分摊项
        assertEquals(null, result.items().get(0).discountDetail().get("coupon"));
    }

    @Test
    void categoryRange_matchesByGoodsCategory() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("分类券", "reduce",
                new BigDecimal("10.00"), null, BigDecimal.ZERO, "category", List.of(10L));
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 100L, "unused", null, snap));

        OrderPriceResult result = calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(
                        item(1, new BigDecimal("100"), 1, List.of(20L)),   // 不属于分类10
                        item(2, new BigDecimal("100"), 1, List.of(10L))),  // 属于分类10
                        "express", null, 1L, null, "none", null));

        assertEquals(new BigDecimal("10.00"), result.couponPrice());
        assertEquals(new BigDecimal("190.00"), result.payPrice());
    }

    @Test
    void thresholdNotMet_throws() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("满1000减10", "reduce",
                new BigDecimal("10.00"), null, new BigDecimal("1000.00"), "all", null);
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 100L, "unused", null, snap));

        assertThrows(BusinessException.class, () -> calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1, null)),
                        "express", null, 1L, null, "none", null)));
    }

    @Test
    void notOwnedByUser_throws() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("券", "reduce",
                new BigDecimal("10.00"), null, BigDecimal.ZERO, "all", null);
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 999L, "unused", null, snap)); // 属于 999 用户

        assertThrows(BusinessException.class, () -> calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1, null)),
                        "express", null, 1L, null, "none", null)));
    }

    @Test
    void expiredCoupon_throws() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("券", "reduce",
                new BigDecimal("10.00"), null, BigDecimal.ZERO, "all", null);
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 100L, "unused", LocalDateTime.now().minusDays(1), snap));

        assertThrows(BusinessException.class, () -> calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1, null)),
                        "express", null, 1L, null, "none", null)));
    }

    @Test
    void alreadyUsedCoupon_throws() {
        UserCouponService userCouponService = mock(UserCouponService.class);
        CouponSnapshot snap = new CouponSnapshot("券", "reduce",
                new BigDecimal("10.00"), null, BigDecimal.ZERO, "all", null);
        when(userCouponService.getByIdWithTenant(1L))
                .thenReturn(userCoupon(1L, 100L, "used", null, snap));

        assertThrows(BusinessException.class, () -> calculator(userCouponService).calculate(
                new PriceContext(1L, 100L, List.of(item(1, new BigDecimal("100"), 1, null)),
                        "express", null, 1L, null, "none", null)));
    }
}
