package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 签到配置保存请求。continuousRules 为连续奖励阶梯：
 * {"days":3,"type":"points","value":5} 或 {"days":7,"type":"coupon","couponId":123}
 */
public record SaveSignConfigRequest(
        @NotNull Integer dailyPoints,
        List<ContinuousRuleItem> continuousRules) {

    public record ContinuousRuleItem(Integer days, String type, Integer value, Long couponId) {}
}
