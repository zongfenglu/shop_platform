package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ApplyAfterSaleRequest(
        @NotNull Long orderId,
        @NotNull Long orderGoodsId,
        @NotBlank String type,
        @NotBlank String applyReason,
        String applyDesc,
        List<String> images,
        Integer refundNum
) {
}
