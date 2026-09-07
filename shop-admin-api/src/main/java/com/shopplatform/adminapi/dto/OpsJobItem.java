package com.shopplatform.adminapi.dto;

import java.time.LocalDateTime;

public record OpsJobItem(
        String code,
        String name,
        String cron,
        boolean implemented,
        boolean runnable,
        String lastStatus,
        String lastMessage,
        Long lastDurationMs,
        LocalDateTime lastRunAt
) {
}
