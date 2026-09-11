package com.shopplatform.clientapi.dto;

import java.math.BigDecimal;

public record PrepayResponse(
        String h5Url,
        String orderNo,
        BigDecimal payPrice,
        boolean simulated
) {
}
