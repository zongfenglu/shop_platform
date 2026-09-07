package com.shopplatform.domain.goods.service;

import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品发布/编辑服务。
 * <p>
 * 核心约束（文档三 §3.2）：单规格商品也生成一条 SKU（{@code spec_value_ids = ''}），
 * 下单、库存扣减、价格计算等所有后续链路只认 SKU，不感知"这个商品到底是不是多规格"——
 * 单规格分支只存在于本服务的"发布"这一步，往后一律没有单规格特判代码。
 */
public interface GoodsService extends TenantSafeService<Goods> {

    /**
     * 发布商品。{@code skuItems} 为空或只有一项且规格值为空时，按单规格处理
     * （生成一条 spec_value_ids='' 的 SKU）；否则按多规格处理（每个 SkuItem 生成一条对应 SKU）。
     */
    Goods publishGoods(PublishGoodsCommand command);

    /** 更新商品资料与 SKU 矩阵，保留商品状态、销量等运营字段。 */
    Goods updateGoods(Long goodsId, PublishGoodsCommand command);

    /** 上架/下架/移入回收站 */
    void updateStatus(Long goodsId, String status);

    record PublishGoodsCommand(
            List<Long> categoryIds,
            Long brandId,
            String name,
            String subName,
            String code,
            List<String> images,
            String specType,
            String content,
            List<String> deliveryType,
            Long freightTemplateId,
            BigDecimal freightFee,
            List<Long> serviceIds,
            Boolean isVirtual,
            List<SkuItem> skuItems
    ) {
    }

    record SkuItem(
            /** 有序规格值ID串，如 "12_35"；单规格商品传空字符串或 null */
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
