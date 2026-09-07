package com.shopplatform.adminapi.dto;

import java.time.LocalDateTime;

public record OpsJobRunResult(
        String code,
        String status,
        String message,
        long durationMs,
        LocalDateTime finishedAt
) {
}
