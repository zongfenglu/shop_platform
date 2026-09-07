package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 用户领取的优惠券。snapshot 存领取时的券关键信息（name/type/reduce_price/discount_ratio/
 * min_price/apply_range/apply_range_config），券后续改动不影响已领取的券——与套餐快照同理。
 * status: unused 未使用 / used 已使用 / expired 已过期。
 */
@TableName("user_coupon")
public class UserCoupon extends BaseEntity {

    private Long shopId;
    private Long userId;
    private Long couponId;
    private String snapshot;
    /** unused / used / expired */
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long useOrderId;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public String getSnapshot() { return snapshot; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Long getUseOrderId() { return useOrderId; }
    public void setUseOrderId(Long useOrderId) { this.useOrderId = useOrderId; }
}
