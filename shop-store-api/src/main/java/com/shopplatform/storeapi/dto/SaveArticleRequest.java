package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveArticleRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank String displayMode,
        @NotBlank @Size(max = 500) String coverUrl,
        @NotBlank String content,
        Integer virtualViews,
        @NotBlank String status,
        Integer sortNo) {}
