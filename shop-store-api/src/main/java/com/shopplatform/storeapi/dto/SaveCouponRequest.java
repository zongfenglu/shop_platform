package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaveCouponRequest(
        @NotBlank String name,
        /** reduce / discount */
        @NotBlank String type,
        BigDecimal reducePrice,
        BigDecimal discountRatio,
        BigDecimal minPrice,
        /** fixed / receive */
        @NotBlank String expireType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer expireDays,
        Integer totalNum,
        Integer limitPerUser,
        /** all / category / goods */
        String applyRange,
        List<Long> applyRangeConfig,
        /** on / off */
        String status
) {
}
