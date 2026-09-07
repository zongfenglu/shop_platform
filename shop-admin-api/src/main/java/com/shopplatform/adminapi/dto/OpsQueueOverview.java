package com.shopplatform.adminapi.dto;

import java.util.List;

public record OpsQueueOverview(
        boolean reachable,
        String nameServer,
        String message,
        long totalBacklog,
        List<OpsQueueItem> queues
) {
}
