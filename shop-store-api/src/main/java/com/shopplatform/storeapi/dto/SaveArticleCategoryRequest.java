package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveArticleCategoryRequest(
        @NotBlank @Size(max = 64) String name,
        Integer sortNo,
        Boolean isShow) {}
