package com.shopplatform.domain.marketing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shopplatform.domain.marketing.entity.UserCoupon;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface UserCouponService extends TenantSafeService<UserCoupon> {

    /** 领取优惠券：校验在架/有效期/限领数，原子扣减库存，写入 user_coupon 快照。 */
    UserCoupon receive(Long userId, Long couponId);

    /** 下单核销：unused→used 并绑定订单。返回 false 表示已被核销/已过期/不存在。 */
    boolean tryUse(Long userCouponId, Long orderId);

    /** 订单取消退回券：仅当该券确实由本订单核销时才退回 unused。 */
    void release(Long userCouponId, Long orderId);

    /** 我的优惠券列表，status 可为 null（全部）。 */
    List<UserCoupon> listMyCoupons(Long userId, String status);

    /** 过期定时任务调用：把 end_time 已过且仍 unused 的券置 expired，返回处理数。 */
    int expireOverdue();

    /**
     * 奖励/兑换发券：跳过领券中心的库存扣减和限领检查（奖励渠道独立），
     * 仅校验券存在且在架，复用有效期计算和快照逻辑。
     * 用于连续签到奖励和积分商城兑换发券。
     */
    UserCoupon issueForReward(Long userId, Long couponId);
}
