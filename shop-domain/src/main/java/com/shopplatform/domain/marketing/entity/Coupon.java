package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券。见文档三 §3.5。
 * type=reduce 满减券（reduce_price 固定金额抵扣）；type=discount 折扣券（discount_ratio 打折）。
 * expire_type=fixed 固定时间段（start/end_time）；=receive 领取后 expire_days 天有效。
 * apply_range=all/category/goods，apply_range_config 存分类或商品 id 数组（JSON）。
 */
@TableName("coupon")
public class Coupon extends BaseEntity {

    private Long shopId;
    private String name;
    /** reduce / discount */
    private String type;
    private BigDecimal reducePrice;
    private BigDecimal discountRatio;
    private BigDecimal minPrice;
    /** fixed / receive */
    private String expireType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer expireDays;
    private Integer totalNum;
    private Integer receivedNum;
    private Integer limitPerUser;
    /** all / category / goods */
    private String applyRange;
    private String applyRangeConfig;
    /** on / off / ended */
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getReducePrice() { return reducePrice; }
    public void setReducePrice(BigDecimal reducePrice) { this.reducePrice = reducePrice; }
    public BigDecimal getDiscountRatio() { return discountRatio; }
    public void setDiscountRatio(BigDecimal discountRatio) { this.discountRatio = discountRatio; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public String getExpireType() { return expireType; }
    public void setExpireType(String expireType) { this.expireType = expireType; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getExpireDays() { return expireDays; }
    public void setExpireDays(Integer expireDays) { this.expireDays = expireDays; }
    public Integer getTotalNum() { return totalNum; }
    public void setTotalNum(Integer totalNum) { this.totalNum = totalNum; }
    public Integer getReceivedNum() { return receivedNum; }
    public void setReceivedNum(Integer receivedNum) { this.receivedNum = receivedNum; }
    public Integer getLimitPerUser() { return limitPerUser; }
    public void setLimitPerUser(Integer limitPerUser) { this.limitPerUser = limitPerUser; }
    public String getApplyRange() { return applyRange; }
    public void setApplyRange(String applyRange) { this.applyRange = applyRange; }
    public String getApplyRangeConfig() { return applyRangeConfig; }
    public void setApplyRangeConfig(String applyRangeConfig) { this.applyRangeConfig = applyRangeConfig; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
