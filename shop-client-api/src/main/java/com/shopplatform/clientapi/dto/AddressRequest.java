package com.shopplatform.clientapi.dto;

public record AddressRequest(
        String name,
        String phone,
        String province,
        String city,
        String region,
        String detail
) {
}
