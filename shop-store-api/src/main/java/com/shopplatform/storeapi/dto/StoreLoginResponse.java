package com.shopplatform.storeapi.dto;

public record StoreLoginResponse(
        String token,
        Long shopId,
        String shopCode,
        Long userId,
        String username,
        String realName,
        boolean platformImpersonation
) {
}
