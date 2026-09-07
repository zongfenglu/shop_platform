package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 签到配置。每租户一条，StoreSignConfigController GET/POST upsert。
 * continuous_rules 为 JSON 数组：{"days":3,"type":"points","value":5} 或 {"days":7,"type":"coupon","couponId":123}。
 */
@TableName("sign_config")
public class SignConfig extends BaseEntity {

    private Long shopId;
    /** 每日签到积分 */
    private Integer dailyPoints;
    /** 连续签到奖励规则（JSON） */
    private String continuousRules;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Integer getDailyPoints() { return dailyPoints; }
    public void setDailyPoints(Integer dailyPoints) { this.dailyPoints = dailyPoints; }
    public String getContinuousRules() { return continuousRules; }
    public void setContinuousRules(String continuousRules) { this.continuousRules = continuousRules; }
}
