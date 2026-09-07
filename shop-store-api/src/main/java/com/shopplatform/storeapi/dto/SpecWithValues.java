package com.shopplatform.storeapi.dto;

import java.util.List;

/**
 * 规格 + 其规格值列表，供前端一次性拿到"颜色：[杏色,黑色,豆沙绿]"这样的结构，
 * 不需要再对每个规格单独查一次规格值。
 */
public record SpecWithValues(
        Long id,
        String name,
        List<GoodsSpecValueItem> values
) {
    public record GoodsSpecValueItem(Long id, String value) {
    }
}
