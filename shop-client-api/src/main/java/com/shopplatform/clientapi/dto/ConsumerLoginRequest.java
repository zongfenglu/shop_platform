package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsumerLoginRequest(
        @NotBlank String mobile
) {
}
