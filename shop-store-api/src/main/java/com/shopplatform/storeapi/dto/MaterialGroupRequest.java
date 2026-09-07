package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

public record MaterialGroupRequest(
        @NotBlank(message = "分组名称不能为空") String name
) {
}
