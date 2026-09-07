package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PublishCommentRequest(
        @NotNull Long orderId,
        @NotNull Long orderGoodsId,
        @NotNull @Min(1) @Max(5) Integer score,
        String content,
        List<String> images
) {
}
