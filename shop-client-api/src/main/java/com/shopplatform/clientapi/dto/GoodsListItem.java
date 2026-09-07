package com.shopplatform.clientapi.dto;

import java.math.BigDecimal;

/**
 * 商品列表项。刻意不直接返回 {@code Goods} 实体：
 * 实体里的 content（详情富文本，可能几十KB）、costPrice 关联、salesActual 真实销量等
 * 要么是列表页用不上的负载，要么是不该暴露给消费者的商家内部数据。
 */
public record GoodsListItem(
        Long id,
        String name,
        String subName,
        /** 封面图（images JSON 数组的第一张） */
        String image,
        /** 最低 SKU 价，多规格商品即"￥xx 起" */
        BigDecimal price,
        /** 最低价 SKU 对应的划线价，可能为 null */
        BigDecimal linePrice,
        /** 展示销量 = 初始基数 + 真实销量，与商家在后台看到的真实销量不是一个口径 */
        Integer sales,
        Integer stockTotal,
        String specType
) {
}
