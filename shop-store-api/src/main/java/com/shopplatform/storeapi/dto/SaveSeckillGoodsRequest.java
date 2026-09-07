package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaveSeckillGoodsRequest(
        @NotNull Long activeId,
        @NotNull Long goodsId,
        @NotNull Long skuId,
        @NotNull BigDecimal seckillPrice,
        /** 秒杀限量，0=不限（限时折扣可填0） */
        Integer seckillNum,
        /** 每人限购，0=不限 */
        Integer limitPerUser,
        /** on / off */
        String status,
        Integer sort
) {
}
