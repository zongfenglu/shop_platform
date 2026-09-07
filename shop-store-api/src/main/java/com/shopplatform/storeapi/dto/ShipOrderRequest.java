package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ShipOrderRequest(
        @NotBlank(message = "快递公司不能为空") String expressCompany,
        @NotBlank(message = "快递单号不能为空") String expressNo,
        /** 为空表示整单发货；非空表示只发这些 order_goods.id（部分发货） */
        List<Long> orderGoodsIds
) {
}
