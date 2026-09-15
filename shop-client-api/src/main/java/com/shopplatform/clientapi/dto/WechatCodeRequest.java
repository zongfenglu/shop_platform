package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatCodeRequest(
        @NotBlank String code
) {
}
