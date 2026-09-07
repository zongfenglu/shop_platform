package com.shopplatform.adminapi.dto;

import java.util.List;

/**
 * 平台账号分页响应。
 */
public record PlatformUserPageResponse(
        List<PlatformUserItem> records,
        long total,
        long current,
        long size,
        long pages,
        PlatformUserSummary summary
) {
}
