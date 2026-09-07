package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 充值方案保存请求。money=用户实付，giftMoney=赠送余额，giftPoints=赠送积分。 */
public record SaveRechargePlanRequest(
        @NotNull @DecimalMin("0.01") BigDecimal money,
        @NotNull @DecimalMin("0") BigDecimal giftMoney,
        @NotNull @Min(0) Integer giftPoints,
        Integer isShow,
        Integer sort
) {
}
