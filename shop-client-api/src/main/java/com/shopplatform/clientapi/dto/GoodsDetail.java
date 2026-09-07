package com.shopplatform.clientapi.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情。规格选择器需要的三样东西一次给全，避免详情页开 3 个请求：
 * {@link #specs} 规格树（渲染可选按钮）、{@link #skus} SKU 列表（按 specValueIds 联动价格/库存）、
 * 商品本身的图文。
 * <p>
 * 联动约定：前端把用户选中的规格值 id 按 {@link SpecGroup} 在本列表中的顺序拼成 {@code "12_35"}，
 * 与 {@link SkuItem#specValueIds} 精确匹配即得到当前 SKU。顺序由后端保证（见
 * {@code ConsumerGoodsController#buildSpecGroups} 的注释），前端不要自己排序。
 */
public record GoodsDetail(
        Long id,
        String name,
        String subName,
        List<String> images,
        String video,
        String content,
        String specType,
        String status,
        Integer sales,
        Integer stockTotal,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal linePrice,
        List<String> deliveryType,
        BigDecimal freightFee,
        Long freightTemplateId,
        Boolean isVirtual,
        List<SpecGroup> specs,
        List<SkuItem> skus
) {

    public record SpecGroup(Long specId, String name, List<SpecValueItem> values) {
    }

    public record SpecValueItem(Long id, String value) {
    }

    public record SkuItem(
            Long id,
            /** 有序规格值ID串，如 "12_35"；单规格商品为空串 */
            String specValueIds,
            String specText,
            BigDecimal price,
            BigDecimal linePrice,
            Integer stock,
            String image
    ) {
    }
}
