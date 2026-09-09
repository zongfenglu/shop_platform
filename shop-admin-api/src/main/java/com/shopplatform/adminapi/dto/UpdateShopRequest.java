package com.shopplatform.adminapi.dto;

import jakarta.validation.constraints.Pattern;

/**
 * 商城基本信息修改请求（平台超管）。
 * null 字段表示不修改，保留原值。
 * 提交时至少有一个字段非 null（业务校验由 Service 层保证）。
 */
public record UpdateShopRequest(

        String name,

        /** 域名前缀：小写字母/数字/短横线，3-32 位。null=不改。 */
        @Pattern(regexp = "^[a-z0-9-]{3,32}$",
                message = "二级域名前缀仅支持小写字母、数字、短横线，长度3-32")
        String code,

        String industry,

        String contact,

        String mobile,

        String remark
) {
}
