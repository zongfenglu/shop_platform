package com.shopplatform.adminapi.dto;

/**
 * 平台账号统计。
 */
public record PlatformUserSummary(
        long total,
        long activeCount,
        long disabledCount
) {
}
