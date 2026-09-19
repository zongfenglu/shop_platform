package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AddressRequest(
        String name,
        String phone,
        String province,
        String city,
        String region,
        String detail,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude
) {
}
