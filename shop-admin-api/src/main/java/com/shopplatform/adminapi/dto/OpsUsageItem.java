package com.shopplatform.adminapi.dto;

import java.time.LocalDate;

public record OpsUsageItem(
        Long shopId,
        String shopName,
        String shopStatus,
        LocalDate statDate,
        Integer goodsCount,
        Integer staffCount,
        Long storageBytes,
        Integer smsMonthUsed
) {
}
