package com.shopplatform.clientapi.dto;

import java.math.BigDecimal;

public record SubmitOrderResponse(
        Long orderId,
        String orderNo,
        BigDecimal payPrice
) {
}
