package com.shopplatform.domain.offlinestore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/** 自提核销记录，一次成功核销一条，order_id 唯一（防重复核销的兜底）。 */
@TableName("verify_log")
public class VerifyLog extends BaseEntity {

    private Long shopId;
    private Long orderId;
    private Long storeId;
    /** store_user.id，执行核销的店员/店主 */
    private Long clerkId;
    private String verifyCode;
    private LocalDateTime verifyTime;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getClerkId() { return clerkId; }
    public void setClerkId(Long clerkId) { this.clerkId = clerkId; }
    public String getVerifyCode() { return verifyCode; }
    public void setVerifyCode(String verifyCode) { this.verifyCode = verifyCode; }
    public LocalDateTime getVerifyTime() { return verifyTime; }
    public void setVerifyTime(LocalDateTime verifyTime) { this.verifyTime = verifyTime; }
}
