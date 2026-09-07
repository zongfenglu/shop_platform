package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.member.entity.RechargeOrder;
import com.shopplatform.domain.member.entity.RechargePlan;
import com.shopplatform.domain.member.mapper.RechargeOrderMapper;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.RechargeOrderService;
import com.shopplatform.domain.member.service.RechargePlanService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RechargeOrderServiceImpl extends ServiceImpl<RechargeOrderMapper, RechargeOrder> implements RechargeOrderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RechargePlanService rechargePlanService;
    private final MemberService memberService;

    public RechargeOrderServiceImpl(RechargePlanService rechargePlanService, MemberService memberService) {
        this.rechargePlanService = rechargePlanService;
        this.memberService = memberService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder createOrder(Long userId, Long planId) {
        RechargePlan plan = rechargePlanService.getByIdWithTenant(planId);
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setPayPrice(plan.getMoney());
        order.setGiftMoney(plan.getGiftMoney() == null ? BigDecimal.ZERO : plan.getGiftMoney());
        order.setGiftPoints(plan.getGiftPoints() == null ? 0 : plan.getGiftPoints());
        order.setPayStatus("unpaid");
        this.save(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(Long orderId, String transactionId, String payMethod) {
        RechargeOrder order = this.getByIdWithTenant(orderId);
        if ("paid".equals(order.getPayStatus())) {
            return false;
        }
        boolean updated = this.update(Wrappers.<RechargeOrder>lambdaUpdate()
                .eq(RechargeOrder::getId, orderId)
                .eq(RechargeOrder::getPayStatus, "unpaid")
                .set(RechargeOrder::getPayStatus, "paid")
                .set(RechargeOrder::getPayMethod, payMethod)
                .set(RechargeOrder::getTransactionId, transactionId)
                .set(RechargeOrder::getPayTime, LocalDateTime.now()));
        if (!updated) {
            // 并发场景：另一条回调先一步入账，按已处理对待
            return false;
        }
        // 入账：到账金额 = 实付 + 赠送余额；积分 = 赠送积分
        BigDecimal credit = order.getPayPrice().add(order.getGiftMoney());
        memberService.adjustBalance(order.getUserId(), credit, "recharge", "余额充值", orderId);
        if (order.getGiftPoints() > 0) {
            memberService.adjustPoints(order.getUserId(), order.getGiftPoints(), "recharge", "充值赠送积分");
        }
        // 充值也累计成长值（按到账金额），让充值用户也能升级
        memberService.addGrowth(order.getUserId(), credit.intValue());
        memberService.upgradeGrade(order.getUserId());
        return true;
    }

    /** RC{yyyyMMdd}{shopId后4位}{随机8位}，带 RC 前缀便于与商品订单号区分。 */
    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(DATE_FMT);
        long shopId = TenantContext.getRequired();
        String shopPart = String.format("%04d", shopId % 10000);
        String randomPart = String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
        return "RC" + datePart + shopPart + randomPart;
    }
}
