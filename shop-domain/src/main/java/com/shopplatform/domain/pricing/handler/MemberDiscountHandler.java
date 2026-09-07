package com.shopplatform.domain.pricing.handler;

import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.UserGradeService;
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
 * 责任链第3节点：会员等级折扣。见文档三 §4。
 * <p>
 * 按用户当前等级 user_grade.discount_ratio 对"非活动商品"打折：
 * "活动商品默认不叠加会员折扣"——即已被 ActivityPriceHandler 换过价（discountDetail 含 "activity"）的行跳过，
 * 这与文档三 §4 "活动商品默认不叠加，可配置" 的默认口径一致。
 * <p>
 * 折扣额 = 行当前小计 × (1 - ratio)，按各行当前小计占比分摊到 discountDetail["memberDiscount"]。
 * ratio 为 null 或 &gt;= 1.00 时不生效（无折扣）。
 */
@Component
public class MemberDiscountHandler implements PriceHandler {

    private final MemberService memberService;
    private final UserGradeService userGradeService;

    public MemberDiscountHandler(MemberService memberService, UserGradeService userGradeService) {
        this.memberService = memberService;
        this.userGradeService = userGradeService;
    }

    @Override
    public PriceHandlerType type() {
        return PriceHandlerType.MEMBER_DISCOUNT;
    }

    @Override
    public void handle(PriceContext ctx, PriceWorkingState state) {
        if (ctx.userId() == null) {
            return;
        }
        // getByIdWithTenant 在记录不存在时会抛越权异常；会员折扣属于"有则享、无则跳过"的增量优惠，
        // 不应因会员/等级缺失而让整个算价失败（如游客预览），故捕获后静默跳过。
        Member member;
        try {
            member = memberService.getByIdWithTenant(ctx.userId());
        } catch (TenantAccessDeniedException e) {
            return;
        }
        if (member.getGradeId() == null) {
            return;
        }
        UserGrade grade;
        try {
            grade = userGradeService.getByIdWithTenant(member.getGradeId());
        } catch (TenantAccessDeniedException e) {
            return;
        }
        BigDecimal ratio = grade.getDiscountRatio();
        if (ratio == null || ratio.compareTo(BigDecimal.ONE) >= 0 || ratio.signum() <= 0) {
            return;
        }

        List<PriceWorkingState.WorkingItem> participating = new ArrayList<>();
        List<BigDecimal> weights = new ArrayList<>();
        for (PriceWorkingState.WorkingItem item : state.items()) {
            // 活动商品不叠加会员折扣
            if (item.discountDetail().containsKey("activity")) {
                continue;
            }
            BigDecimal sub = item.currentSubtotal();
            if (sub.signum() <= 0) {
                continue;
            }
            participating.add(item);
            weights.add(sub);
        }
        if (participating.isEmpty()) {
            return;
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            // 单行折扣 = 小计 × (1 - ratio)
            BigDecimal lineDiscount = w.multiply(BigDecimal.ONE.subtract(ratio))
                    .setScale(2, RoundingMode.HALF_UP);
            totalDiscount = totalDiscount.add(lineDiscount);
        }
        if (totalDiscount.signum() <= 0) {
            return;
        }
        // 不得超过参与商品小计
        BigDecimal subtotal = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            subtotal = subtotal.add(w);
        }
        if (totalDiscount.compareTo(subtotal) > 0) {
            totalDiscount = subtotal;
        }
        DiscountApportioner.apportion(participating, weights, totalDiscount, "memberDiscount");
    }
}
