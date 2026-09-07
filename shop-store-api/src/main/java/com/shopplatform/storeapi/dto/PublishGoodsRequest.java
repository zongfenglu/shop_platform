package com.shopplatform.storeapi.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发布商品请求。对应原型 store/goods-edit.html：基本信息 + 单/多规格切换 + SKU矩阵 + 物流。
 */
public record PublishGoodsRequest(
        List<Long> categoryIds,
        Long brandId,
        String name,
        String subName,
        String code,
        List<String> images,
        /** single单规格 / multi多规格 */
        String specType,
        String content,
        List<String> deliveryType,
        Long freightTemplateId,
        BigDecimal freightFee,
        List<Long> serviceIds,
        Boolean isVirtual,
        List<SkuItemDto> skuItems
) {
    public record SkuItemDto(
            String specValueIds,
            String skuCode,
            BigDecimal price,
            BigDecimal linePrice,
            BigDecimal costPrice,
            Integer stock,
            BigDecimal weight,
            BigDecimal volume,
            String image
    ) {
    }
}
