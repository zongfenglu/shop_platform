package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * rules/freeRules 直接透传 JSON 字符串，格式见文档三 §3.6（阶梯计费规则 / 包邮规则）。
 */
public record CreateFreightTemplateRequest(
        @NotBlank(message = "模板名称不能为空") String name,
        @NotBlank(message = "计费方式不能为空") String method,
        @NotBlank(message = "计费规则不能为空") String rules,
        String freeRules
) {
}
