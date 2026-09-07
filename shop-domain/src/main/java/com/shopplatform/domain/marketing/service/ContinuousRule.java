package com.shopplatform.domain.marketing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * 连续签到奖励规则项。
 * type=points → value 直接发放积分；type=coupon → couponId 发券。
 * JSON 示例：{"days":3,"type":"points","value":5} 或 {"days":7,"type":"coupon","couponId":123}
 */
public record ContinuousRule(Integer days, String type, Integer value, Long couponId) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 从 JSON 字符串解析规则列表，容错：格式错误返回空列表。 */
    public static List<ContinuousRule> parseList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** 将规则列表序列化为 JSON 字符串。 */
    public static String toJson(List<ContinuousRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(rules);
        } catch (Exception e) {
            return "[]";
        }
    }
}
