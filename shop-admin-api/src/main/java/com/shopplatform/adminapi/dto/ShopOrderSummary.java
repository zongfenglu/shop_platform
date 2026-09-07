package com.shopplatform.adminapi.dto;

/**
 * 平台订单汇总。
 */
public record ShopOrderSummary(
        long total,
        long pending,
        long paid,
        long refunded,
        long newCount,
        long renewCount,
        long upgradeCount,
        long addonCount
) {
}
