package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotNull;

/** 后台调整会员积分。value 带正负号。 */
public record AdjustPointsRequest(
        @NotNull Long userId,
        @NotNull Integer value,
        String remark
) {
}
