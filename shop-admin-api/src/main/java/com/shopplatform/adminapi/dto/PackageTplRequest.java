package com.shopplatform.adminapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 套餐模板新建/编辑请求。对应原型 admin/package-list.html 的编辑弹窗。
 * <p>
 * menus/quota/price 前端以对象形式提交，这里用 String 接收原始 JSON 文本再转存——
 * package_tpl 表这三列本来就是 JSON 类型，没必要在后端定义与前端强耦合的强类型 DTO，
 * 字段集合本身就会随套餐能力扩展而变化（见文档一 §3.4）。
 */
public record PackageTplRequest(
        @NotBlank(message = "套餐名称不能为空")
        String name,

        String intro,

        @NotNull(message = "功能项配置不能为空")
        String menus,

        @NotNull(message = "配额配置不能为空")
        String quota,

        @NotNull(message = "价格配置不能为空")
        String price,

        Boolean isTrial,

        Boolean isShow,

        Integer sort
) {
}
