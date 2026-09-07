package com.shopplatform.adminapi.dto;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String realName
) {
}
