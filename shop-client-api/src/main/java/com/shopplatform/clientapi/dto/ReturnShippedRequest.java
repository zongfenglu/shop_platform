package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.NotBlank;

public record ReturnShippedRequest(
        @NotBlank String expressCompany,
        @NotBlank String expressNo
) {
}
