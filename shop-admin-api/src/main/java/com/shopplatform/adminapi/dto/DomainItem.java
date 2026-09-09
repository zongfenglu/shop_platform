package com.shopplatform.adminapi.dto;

import java.time.LocalDateTime;

/**
 * 平台域名条目。
 */
public record DomainItem(
        Long id,
        Long shopId,
        String shopName,
        String domain,
        String protocol,
        String type,
        String certStatus,
        LocalDateTime certExpireTime,
        String verifyStatus,
        String cnameTarget,
        String cnameStatus,
        String rejectReason,
        String certError,
        LocalDateTime createTime
) {
}
