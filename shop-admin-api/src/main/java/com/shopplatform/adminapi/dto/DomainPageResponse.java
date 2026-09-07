package com.shopplatform.adminapi.dto;

import java.util.List;

/**
 * 平台域名分页响应。
 */
public record DomainPageResponse(
        List<DomainItem> records,
        long total,
        long current,
        long size,
        long pages,
        DomainSummary summary
) {
}
