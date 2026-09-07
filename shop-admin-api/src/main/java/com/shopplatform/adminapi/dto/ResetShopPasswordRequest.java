package com.shopplatform.adminapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 平台超管重置商城店主密码。 */
public record ResetShopPasswordRequest(
        @NotBlank(message = "请输入新密码")
        @Size(min = 8, max = 72, message = "密码长度需为 8～72 位")
        String password
) {
}
