package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        Long parentId,
        @NotBlank(message = "分类名称不能为空") @Size(max = 64, message = "分类名称不能超过 64 字") String name,
        @Size(max = 255) String image,
        Integer sort,
        Boolean isShow
) {
}
