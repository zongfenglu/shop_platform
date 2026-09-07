package com.shopplatform.adminapi.dto;

import java.util.List;

/**
 * 平台订单分页响应。
 */
public record ShopOrderPageResponse(
        List<ShopOrderItem> records,
        long total,
        long current,
        long size,
        long pages,
        ShopOrderSummary summary
) {
}
