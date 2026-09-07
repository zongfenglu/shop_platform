package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户后台保存砍价活动请求。floorPrice 为砍到底价，validHours 为发起后有效时长，helpLimit 为助力次数上限（0=不限）。
 */
public record SaveBargainActiveRequest(
        @NotNull Long goodsId,
        @NotNull BigDecimal floorPrice,
        @NotNull Integer validHours,
        Integer helpLimit,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String status
) {
}
