package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

/** 给某个规格新增一个规格值（如给"颜色"加"杏色"）。对应原型的"＋ 添加规格值"标签。 */
public record CreateSpecValueRequest(
        @NotBlank(message = "规格值不能为空") String value
) {
}
