package com.shopplatform.adminapi.dto;

import java.time.LocalDateTime;

public record SysLogItem(
        Long id,
        Long shopId,
        String shopName,
        Integer operatorType,
        String operatorName,
        Boolean byPlatform,
        String action,
        String description,
        String ip,
        LocalDateTime createTime
) {
}
