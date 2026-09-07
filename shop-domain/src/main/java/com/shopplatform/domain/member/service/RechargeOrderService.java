package com.shopplatform.domain.member.service;

import com.shopplatform.domain.member.entity.RechargeOrder;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface RechargeOrderService extends TenantSafeService<RechargeOrder> {

    /** 创建充值订单（unpaid）。用户选择方案后调用，随后由支付流程触发 markPaid 入账。 */
    RechargeOrder createOrder(Long userId, Long planId);

    /**
     * 充值单支付成功入账：unpaid→paid（乐观锁，幂等），成功后给会员余额/积分入账并写流水。
     *
     * @return 首次入账返回 true，重复回调/查单返回 false（调用方据此判断是否要发通知等副作用）
     */
    boolean markPaid(Long orderId, String transactionId, String payMethod);
}
