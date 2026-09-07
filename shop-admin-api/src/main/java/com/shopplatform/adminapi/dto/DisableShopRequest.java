package com.shopplatform.adminapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 平台超管停用商城。 */
public record DisableShopRequest(
        @NotBlank(message = "请填写停用原因")
        @Size(max = 255, message = "停用原因不能超过 255 字")
        String reason
) {
}
