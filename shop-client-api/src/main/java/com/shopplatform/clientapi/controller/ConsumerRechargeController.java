package com.shopplatform.clientapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.member.entity.RechargeOrder;
import com.shopplatform.domain.member.entity.RechargePlan;
import com.shopplatform.domain.member.service.RechargeOrderService;
import com.shopplatform.domain.member.service.RechargePlanService;
import com.shopplatform.framework.security.LoginUserContext;
import com.shopplatform.clientapi.dto.CreateRechargeOrderRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消费者端余额充值：查看充值方案、创建充值单、（简化）支付入账。
 * 对应原型 h5/my.html 的充值入口。
 * <p>
 * 正式支付链路与商品订单一致（微信支付 prepay → 回调 → markPaid），本阶段为让充值闭环可演示，
 * 暂提供 {@code /pay} 端点直接走"模拟支付成功"入账；接入微信支付后，将该端点替换为 prepay + 回调即可，
 * RechargeOrderService.markPaid 的入账逻辑（余额/积分/成长值）不变。
 */
@RestController
@RequestMapping("/api/recharge")
public class ConsumerRechargeController {

    private final RechargePlanService rechargePlanService;
    private final RechargeOrderService rechargeOrderService;

    public ConsumerRechargeController(RechargePlanService rechargePlanService, RechargeOrderService rechargeOrderService) {
        this.rechargePlanService = rechargePlanService;
        this.rechargeOrderService = rechargeOrderService;
    }

    /** 上架的充值方案列表。 */
    @GetMapping("/plans")
    public Result<List<RechargePlan>> plans() {
        return Result.ok(rechargePlanService.listShown());
    }

    /** 创建充值订单（unpaid），返回订单号供后续支付。 */
    @PostMapping("/orders")
    public Result<RechargeOrder> createOrder(@Valid @RequestBody CreateRechargeOrderRequest req) {
        Long userId = requireLoginUserId();
        return Result.ok(rechargeOrderService.createOrder(userId, req.planId()));
    }

    /**
     * 模拟支付成功：直接对充值单入账（unpaid→paid + 余额/积分/成长值）。
     * 接入微信支付后此端点应替换为 prepay + 回调，markPaid 入账逻辑复用。
     */
    @PostMapping("/orders/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        requireLoginUserId();
        boolean ok = rechargeOrderService.markPaid(id, "MOCK_" + System.currentTimeMillis(), "mock");
        if (!ok) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "充值单已支付或不可支付");
        }
        return Result.ok();
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser.userId();
    }
}
