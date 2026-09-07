package com.shopplatform.adminapi.dto;

/**
 * 平台域名统计。
 */
public record DomainSummary(
        long total,
        long customCount,
        long subCount,
        long pendingCount,
        long validCount
) {
}
