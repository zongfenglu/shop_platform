package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 积分商城兑换项保存请求。goodsId/couponId 二选一。
 */
public record SavePointsGoodsRequest(
        @NotBlank String name,
        String image,
        Long goodsId,
        Long couponId,
        @NotNull Integer points,
        BigDecimal cash,
        Integer stock,
        /** on / off */
        String status,
        Integer sort) {
}
