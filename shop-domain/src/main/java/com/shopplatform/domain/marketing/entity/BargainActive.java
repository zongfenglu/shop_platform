package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 砍价活动。见文档三 §3.5、Sprint 10。
 * 用户发起砍价后 valid_hours 内可邀请好友助力，current_price 逐刀降至 floor_price 或达 help_limit 为止。
 * 用户以 current_price 下单（activityType=bargain，activityId=bargain_record.id）。
 */
@TableName("bargain_active")
public class BargainActive extends BaseEntity {

    private Long shopId;
    private Long goodsId;
    private BigDecimal floorPrice;
    private Integer validHours;
    /** 助力次数上限，0=不限 */
    private Integer helpLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** on / off */
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public BigDecimal getFloorPrice() { return floorPrice; }
    public void setFloorPrice(BigDecimal floorPrice) { this.floorPrice = floorPrice; }
    public Integer getValidHours() { return validHours; }
    public void setValidHours(Integer validHours) { this.validHours = validHours; }
    public Integer getHelpLimit() { return helpLimit; }
    public void setHelpLimit(Integer helpLimit) { this.helpLimit = helpLimit; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
