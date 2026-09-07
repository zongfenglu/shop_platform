package com.shopplatform.adminapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 新建商城请求。对应原型 admin/shop-new.html 的5步向导（本接口一次性提交，
 * 前端向导只是分步收集字段，最终提交时聚合为一个请求）。
 */
public record CreateShopRequest(

        @NotBlank(message = "商城名称不能为空")
        String name,

        @NotBlank(message = "二级域名前缀不能为空")
        @Pattern(regexp = "^[a-z0-9-]{3,32}$", message = "二级域名前缀仅支持小写字母、数字、短横线，长度3-32")
        String code,

        String industry,

        String contact,

        String mobile,

        String remark,

        @NotNull(message = "请选择套餐")
        Long packageTplId,

        @NotNull(message = "请选择订购周期")
        Integer durationMonth
) {
}
