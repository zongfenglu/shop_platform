package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record SaveSeckillActiveRequest(
        @NotBlank String name,
        /** 场次id数组；空/null = 限时折扣（全天有效） */
        List<Long> timeIds,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        /** on / off */
        String status,
        String remark
) {
}
