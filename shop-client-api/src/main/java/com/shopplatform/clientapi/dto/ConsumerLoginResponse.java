package com.shopplatform.clientapi.dto;

public record ConsumerLoginResponse(
        String token,
        Long userId,
        String nickname
) {
}
