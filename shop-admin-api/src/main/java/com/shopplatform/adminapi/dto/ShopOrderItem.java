package com.shopplatform.adminapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台订单列表项。
 */
public record ShopOrderItem(
        Long id,
        String orderNo,
        Long shopId,
        String shopName,
        String type,
        Long packageTplId,
        String packageName,
        Integer durationMonth,
        BigDecimal amount,
        String payStatus,
        String payMethod,
        LocalDateTime payTime,
        LocalDateTime createTime
) {
}
