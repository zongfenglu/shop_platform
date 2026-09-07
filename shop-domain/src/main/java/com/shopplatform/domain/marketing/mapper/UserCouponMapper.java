package com.shopplatform.domain.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.marketing.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /** 领券：coupon.received_num + 1，带 total_num 上限校验（0=不限）。返回 0 表示已发完。 */
    @Update("UPDATE coupon SET received_num = received_num + 1 WHERE id = #{couponId} AND (total_num = 0 OR received_num < total_num)")
    int incrReceived(@Param("couponId") Long couponId);

    /** 核销：user_coupon unused→used，带 use_order_id。返回 0 表示已核销/已过期/不存在。 */
    @Update("UPDATE user_coupon SET status = 'used', use_order_id = #{orderId} WHERE id = #{id} AND status = 'unused'")
    int markUsed(@Param("id") Long id, @Param("orderId") Long orderId);

    /** 退回：user_coupon used→unused（订单取消时），仅限本订单核销的券。 */
    @Update("UPDATE user_coupon SET status = 'unused', use_order_id = NULL WHERE id = #{id} AND status = 'used' AND use_order_id = #{orderId}")
    int releaseUsed(@Param("id") Long id, @Param("orderId") Long orderId);

    /** 过期：end_time 已过且仍 unused 的券置 expired。返回处理行数。 */
    @Update("UPDATE user_coupon SET status = 'expired' WHERE status = 'unused' AND end_time IS NOT NULL AND end_time < #{now}")
    int expireOverdue(@Param("now") java.time.LocalDateTime now);
}
