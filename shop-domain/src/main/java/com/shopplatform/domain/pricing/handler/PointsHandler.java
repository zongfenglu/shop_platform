package com.shopplatform.domain.pricing.handler;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.pricing.DiscountApportioner;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.domain.pricing.PriceHandler;
import com.shopplatform.domain.pricing.PriceHandlerType;
import com.shopplatform.domain.pricing.PriceWorkingState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 责任链第6节点：积分抵扣。见文档三 §4。
 * <p>
 * 按 ctx.pointsToUse() 积分数换算抵扣金额：抵扣额 = pointsToUse / exchangeRate（默认 100 积分=1 元），
 * 受最高抵扣比例 maxRatio（默认 0.5，即可抵扣商品总额的一半）限制，且不超过用户可用积分换算值。
 * <p>
 * 抵扣额按各行当前小计占比分摊到 discountDetail["points"]。
 * 本 Handler 只做"算价 + 校验"，不扣减积分（积分扣减由 {@code OrderService} 在下单成功后调用
 * {@code MemberService.adjustPoints}，保证预览无副作用）。
 * <p>
 * exchangeRate / maxRatio 通过 application.yml 的 shop.points.exchange-rate / shop.points.max-ratio 配置，
 * 暂未接入 store_setting（M2 后续补），先用全局默认值。
 */
@Component
public class PointsHandler implements PriceHandler {

    private final MemberService memberService;

    @Value("${shop.points.exchange-rate:100}")
    private int exchangeRate;

    @Value("${shop.points.max-ratio:0.5}")
    private BigDecimal maxRatio;

    public PointsHandler(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public PriceHandlerType type() {
        return PriceHandlerType.POINTS;
    }

    @Override
    public void handle(PriceContext ctx, PriceWorkingState state) {
        if (ctx.pointsToUse() == null || ctx.pointsToUse() <= 0 || exchangeRate <= 0) {
            return;
        }
        // 校验用户积分余额（游客预览无 userId 时跳过余额校验，仅按比例上限计算）
        if (ctx.userId() != null) {
            Member member;
            try {
                member = memberService.getByIdWithTenant(ctx.userId());
            } catch (TenantAccessDeniedException e) {
                return;
            }
            int available = member.getPoints() == null ? 0 : member.getPoints();
            if (available < ctx.pointsToUse()) {
                throw new BusinessException(ErrorCode.POINTS_INSUFFICIENT, "积分不足");
            }
        }

        BigDecimal goodsTotal = state.currentGoodsTotal();
        if (goodsTotal.signum() <= 0) {
            return;
        }

        // 积分换算抵扣额
        BigDecimal byPoints = BigDecimal.valueOf(ctx.pointsToUse())
                .divide(BigDecimal.valueOf(exchangeRate), 2, RoundingMode.DOWN);
        // 最高抵扣比例上限
        BigDecimal byRatio = goodsTotal.multiply(maxRatio).setScale(2, RoundingMode.DOWN);
        BigDecimal discount = byPoints.min(byRatio);
        if (discount.signum() <= 0) {
            return;
        }
        // 不得超过商品总额
        if (discount.compareTo(goodsTotal) > 0) {
            discount = goodsTotal;
        }

        List<PriceWorkingState.WorkingItem> participating = new ArrayList<>();
        List<BigDecimal> weights = new ArrayList<>();
        for (PriceWorkingState.WorkingItem item : state.items()) {
            BigDecimal sub = item.currentSubtotal();
            if (sub.signum() <= 0) {
                continue;
            }
            participating.add(item);
            weights.add(sub);
        }
        DiscountApportioner.apportion(participating, weights, discount, "points");
    }
}
