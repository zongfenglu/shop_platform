package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 砍价记录（一个用户对一个活动一条）。见 Sprint 10。
 * 发起时 current_price=SKU 原价；每次助力 current_price 递减（不低于 floor_price）。
 * status: ongoing(进行中) / done(已砍到底价或达助力上限) / expired(过期) / ordered(已下单)。
 */
@TableName("bargain_record")
public class BargainRecord extends BaseEntity {

    private Long shopId;
    private Long activeId;
    private Long userId;
    private BigDecimal currentPrice;
    private Integer helpCount;
    /** ongoing / done / expired / ordered */
    private String status;
    private LocalDateTime expireTime;
    private Long orderId;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getActiveId() { return activeId; }
    public void setActiveId(Long activeId) { this.activeId = activeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public Integer getHelpCount() { return helpCount; }
    public void setHelpCount(Integer helpCount) { this.helpCount = helpCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
}
