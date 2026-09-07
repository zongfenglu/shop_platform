package com.shopplatform.clientapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 地址簿的新增/修改请求。与 {@link AddressRequest}（下单时直接提交的地址快照）不是一回事：
 * 那个是一次性的订单快照，这个是会被复用的地址簿记录，所以多了 isDefault 且校验更严。
 */
public record SaveAddressRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @Size(max = 32) String province,
        @Size(max = 32) String city,
        @Size(max = 32) String region,
        @NotBlank @Size(max = 255) String detail,
        Boolean isDefault
) {
}
