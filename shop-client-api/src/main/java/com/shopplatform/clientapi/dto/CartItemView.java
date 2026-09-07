package com.shopplatform.clientapi.dto;

import java.math.BigDecimal;

/**
 * 购物车行的展示视图。价格/库存/商品名都是**实时**从 goods_sku / goods 读的，不是加购时的快照
 * （见 V11 迁移注释），所以商家改价、改名、下架都会立刻反映到购物车上。
 */
public record CartItemView(
        Long id,
        Long goodsId,
        Long skuId,
        String goodsName,
        String specText,
        String image,
        BigDecimal price,
        BigDecimal linePrice,
        Integer quantity,
        Integer stock,
        /** 商品已下架或 SKU 已被删除。失效行在前端置灰、不可勾选，且不计入合计。 */
        Boolean invalid,
        String invalidReason
) {
}
