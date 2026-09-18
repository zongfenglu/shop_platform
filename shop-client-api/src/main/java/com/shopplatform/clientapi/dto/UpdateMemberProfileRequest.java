package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberProfileRequest(
        @NotBlank(message = "请输入昵称")
        @Size(max = 32, message = "昵称不能超过32个字符")
        String nickname
) {
}
