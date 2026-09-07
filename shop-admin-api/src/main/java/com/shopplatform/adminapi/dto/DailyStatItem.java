package com.shopplatform.adminapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyStatItem(
        Long shopId,
        String shopName,
        LocalDate statDate,
        Integer orderCount,
        Integer payCount,
        BigDecimal payAmount,
        BigDecimal refundAmount,
        Integer newUser,
        Integer activeUser,
        Integer uv,
        Integer pv
) {
}
