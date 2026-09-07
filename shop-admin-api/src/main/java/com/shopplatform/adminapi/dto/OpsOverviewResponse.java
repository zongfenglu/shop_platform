package com.shopplatform.adminapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpsOverviewResponse(
        long shopTotal,
        long activeShopTotal,
        long pendingDomainTotal,
        long orderTotal,
        long platformUserTotal,
        long totalStorageBytes,
        long totalSmsUsed,
        LocalDateTime generatedAt,
        List<OpsUsageItem> usageRows
) {
}
