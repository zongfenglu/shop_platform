package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 商户后台保存拼团活动请求。groupPrice 为 {skuId: 拼团价}，由 Controller 序列化为 JSON 存 group_active.group_price。
 */
public record SaveGroupActiveRequest(
        @NotNull Long goodsId,
        @NotNull Integer groupNum,
        /** {skuId: price}，至少一个 SKU */
        @NotNull Map<Long, BigDecimal> groupPrice,
        @NotNull Integer validHours,
        Integer isMock,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String status
) {
}
