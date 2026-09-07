package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 会员等级保存请求。discountRatio 1.00=无折扣 0.90=九折。 */
public record SaveUserGradeRequest(
        @NotBlank String name,
        @NotNull @Min(0) Integer weight,
        @NotNull @Min(0) Integer growthValue,
        @NotNull @DecimalMin("0.01") @DecimalMax("1.00") BigDecimal discountRatio,
        String icon,
        String remark
) {
}
