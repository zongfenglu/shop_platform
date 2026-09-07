package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDate;

/**
 * 签到记录。每日一条，uk_shop_user_date 防止重复签到。
 * day_number 存连续天数快照，断签后从 1 重新计。
 */
@TableName("sign_record")
public class SignRecord extends BaseEntity {

    private Long shopId;
    private Long userId;
    /** 签到日期 */
    private LocalDate signDate;
    /** 本次签到时处于的连续天数 */
    private Integer dayNumber;
    /** 本次签到所得积分（含连签奖励） */
    private Integer pointsEarned;
    /** 是否补签 0否 1是 */
    private Integer isMakeup;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getSignDate() { return signDate; }
    public void setSignDate(LocalDate signDate) { this.signDate = signDate; }
    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }
    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
    public Integer getIsMakeup() { return isMakeup; }
    public void setIsMakeup(Integer isMakeup) { this.isMakeup = isMakeup; }
}
