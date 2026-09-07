package com.shopplatform.domain.pricing.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.marketing.entity.UserCoupon;
import com.shopplatform.domain.marketing.service.CouponSnapshot;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.pricing.DiscountApportioner;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.domain.pricing.PriceHandler;
import com.shopplatform.domain.pricing.PriceHandlerType;
import com.shopplatform.domain.pricing.PriceWorkingState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 责任链第5节点：优惠券。见文档三 §4。
 * <p>
 * PriceContext.couponId 传的是 user_coupon.id（用户领取的那一张实例），不是券模板 id。
 * 本 Handler 只做"算价 + 校验"，不写库（核销/退回由 {@code OrderService} 在下单/取消时调用
 * {@code UserCouponService.tryUse / release}），保证预览与下单共用同一套校验且预览无副作用。
 * <p>
 * 校验：券属于当前用户、状态 unused、未过期；按 snapshot.applyRange 选参与商品；
 * 门槛按"参与商品实付小计"（currentSubtotal 之和，即活动价/会员折扣后的当前小计）判定。
 * 命中后把优惠总额按参与行 currentSubtotal 占比分摊到 discountDetail["coupon"]。
 */
@Component
public class CouponHandler implements PriceHandler {

    private final UserCouponService userCouponService;
    private final ObjectMapper objectMapper;

    public CouponHandler(UserCouponService userCouponService, ObjectMapper objectMapper) {
        this.userCouponService = userCouponService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PriceHandlerType type() {
        return PriceHandlerType.COUPON;
    }

    @Override
    public void handle(PriceContext ctx, PriceWorkingState state) {
        if (ctx.couponId() == null) {
            return;
        }
        UserCoupon uc = userCouponService.getByIdWithTenant(ctx.couponId());
        if (uc == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券不存在");
        }
        if (ctx.userId() == null || !ctx.userId().equals(uc.getUserId())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券不属于当前用户");
        }
        if (!"unused".equals(uc.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券已使用或已过期");
        }
        LocalDateTime now = LocalDateTime.now();
        if (uc.getEndTime() != null && now.isAfter(uc.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券已过期");
        }

        CouponSnapshot snap = parseSnapshot(uc.getSnapshot());
        if (snap == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券信息缺失");
        }

        List<PriceWorkingState.WorkingItem> participating = pickParticipating(state.items(), snap);
        if (participating.isEmpty()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "当前商品不适用该优惠券");
        }

        BigDecimal participatingSubtotal = BigDecimal.ZERO;
        List<BigDecimal> weights = new ArrayList<>();
        for (PriceWorkingState.WorkingItem item : participating) {
            BigDecimal sub = item.currentSubtotal();
            participatingSubtotal = participatingSubtotal.add(sub);
            weights.add(sub);
        }

        BigDecimal minPrice = snap.minPrice() == null ? BigDecimal.ZERO : snap.minPrice();
        if (participatingSubtotal.compareTo(minPrice) < 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "未达到优惠券使用门槛");
        }

        BigDecimal discount = computeDiscount(snap, participatingSubtotal);
        if (discount.signum() <= 0) {
            return;
        }
        // 优惠不得超过参与商品小计
        if (discount.compareTo(participatingSubtotal) > 0) {
            discount = participatingSubtotal;
        }

        DiscountApportioner.apportion(participating, weights, discount, "coupon");
    }

    private BigDecimal computeDiscount(CouponSnapshot snap, BigDecimal subtotal) {
        if ("discount".equals(snap.type())) {
            BigDecimal ratio = snap.discountRatio() == null ? BigDecimal.ONE : snap.discountRatio();
            return subtotal.multiply(BigDecimal.ONE.subtract(ratio)).setScale(2, RoundingMode.HALF_UP);
        }
        // reduce：固定金额抵扣
        BigDecimal reduce = snap.reducePrice() == null ? BigDecimal.ZERO : snap.reducePrice();
        return reduce.setScale(2, RoundingMode.HALF_UP);
    }

    private List<PriceWorkingState.WorkingItem> pickParticipating(List<PriceWorkingState.WorkingItem> items,
                                                                     CouponSnapshot snap) {
        String range = snap.applyRange() == null ? "all" : snap.applyRange();
        Set<Long> configIds = snap.applyRangeConfig() == null ? Set.of() : new HashSet<>(snap.applyRangeConfig());
        List<PriceWorkingState.WorkingItem> result = new ArrayList<>();
        for (PriceWorkingState.WorkingItem item : items) {
            switch (range) {
                case "all" -> result.add(item);
                case "goods" -> {
                    if (configIds.contains(item.source().goodsId())) {
                        result.add(item);
                    }
                }
                case "category" -> {
                    List<Long> cats = item.source().categoryIds();
                    if (cats != null && cats.stream().anyMatch(configIds::contains)) {
                        result.add(item);
                    }
                }
                default -> result.add(item);
            }
        }
        return result;
    }

    private CouponSnapshot parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CouponSnapshot.class);
        } catch (Exception e) {
            return null;
        }
    }
}
