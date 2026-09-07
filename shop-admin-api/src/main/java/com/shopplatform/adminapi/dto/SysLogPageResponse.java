package com.shopplatform.adminapi.dto;

import java.util.List;

public record SysLogPageResponse(
        List<SysLogItem> records,
        long total,
        long current,
        long size,
        long pages
) {
}
