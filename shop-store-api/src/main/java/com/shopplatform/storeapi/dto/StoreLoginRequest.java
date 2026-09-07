package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

public record StoreLoginRequest(
        @NotBlank(message = "商城标识不能为空") String shopCode,
        @NotBlank(message = "账号不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {
}
