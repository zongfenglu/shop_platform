package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

/** 新建规格（如"颜色"）。对应原型 store/goods-edit.html 多规格区块的"规格：颜色/尺码"。 */
public record CreateSpecRequest(
        @NotBlank(message = "规格名称不能为空") String name
) {
}
