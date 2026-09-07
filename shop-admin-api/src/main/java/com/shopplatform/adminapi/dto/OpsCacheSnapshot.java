package com.shopplatform.adminapi.dto;

import java.util.List;

public record OpsCacheSnapshot(
        long tenantKeyCount,
        String usedMemory,
        String maxMemory,
        String hitRate,
        List<ShopOption> shops
) {
    public record ShopOption(Long id, String name) {
    }
}
