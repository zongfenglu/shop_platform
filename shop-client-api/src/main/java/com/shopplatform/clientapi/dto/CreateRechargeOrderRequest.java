package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.NotNull;

/** 创建充值订单：选择一个充值方案。 */
public record CreateRechargeOrderRequest(
        @NotNull Long planId
) {
}
