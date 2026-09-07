package com.shopplatform.adminapi.dto;

import jakarta.validation.constraints.NotBlank;

/** 新建/编辑平台管理员 */
public record SavePlatformUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        String realName,
        String mobile,
        Integer status,
        Long roleId) {
}
