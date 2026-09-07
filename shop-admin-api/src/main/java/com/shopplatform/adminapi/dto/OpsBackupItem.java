package com.shopplatform.adminapi.dto;

import java.time.LocalDateTime;

public record OpsBackupItem(
        Long id,
        String filename,
        Long sizeBytes,
        String status,
        String message,
        LocalDateTime createTime
) {
}
