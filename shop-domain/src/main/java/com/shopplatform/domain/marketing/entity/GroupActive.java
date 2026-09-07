package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 拼团活动。见文档三 §3.5、Sprint 10。
 * group_price 为 JSON：{"skuId": 拼团价}，ActivityPriceHandler 在 activityType=group 时按 SKU 取价替换原价。
 * 开团后 valid_hours 内未凑齐 group_num 人则由 GroupExpireJob 原路退款。
 */
@TableName("group_active")
public class GroupActive extends BaseEntity {

    private Long shopId;
    private Long goodsId;
    /** 成团人数（含团长） */
    private Integer groupNum;
    /** JSON：{"skuId": price} */
    private String groupPrice;
    private Integer validHours;
    /** 0否 1是：人数不足时模拟成团（仅演示） */
    private Integer isMock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** on / off */
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public Integer getGroupNum() { return groupNum; }
    public void setGroupNum(Integer groupNum) { this.groupNum = groupNum; }
    public String getGroupPrice() { return groupPrice; }
    public void setGroupPrice(String groupPrice) { this.groupPrice = groupPrice; }
    public Integer getValidHours() { return validHours; }
    public void setValidHours(Integer validHours) { this.validHours = validHours; }
    public Integer getIsMock() { return isMock; }
    public void setIsMock(Integer isMock) { this.isMock = isMock; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
