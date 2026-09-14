package com.shopplatform.domain.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * 价格计算的输入。见文档三 §4：下单预览、正式下单、修改订单、售后退款金额计算
 * 必须共用同一套输入结构，不允许各处自己拼一套"差不多"的上下文。
 */
public record PriceContext(
        Long shopId,
        Long userId,
        List<PriceItem> items,
        /** 门店自提时免运费，见文档三 §4 FreightHandler 说明 */
        String deliveryType,
        Long freightTemplateId,
        Long couponId,
        Integer pointsToUse,
        /** none/seckill/group/bargain，决定 ActivityPriceHandler 是否生效 */
        String activityType,
        Long activityId
) {

    /** 参与计算的一个购物项（购物车行 / 立即购买行），对应最终会落成一条 order_goods。 */
    public record PriceItem(
            Long goodsId,
            Long skuId,
            String goodsName,
            String specText,
            String image,
            /** SKU 原价，来自 goods_sku.price，不接受调用方传入篡改后的价格 */
            BigDecimal skuPrice,
            BigDecimal linePrice,
            Integer quantity,
            BigDecimal weight,
            BigDecimal volume,
            /** 商品所属分类 id 列表，来自 goods.category_ids（JSON 解析后）。优惠券按分类适用时用它判定参与项。 */
            List<Long> categoryIds,
            /** 商品关联的运费模板，必须由服务端商品数据填充，不能信任客户端传值。 */
            Long freightTemplateId,
            /** 商品未使用运费模板时的统一运费。 */
            BigDecimal freightFee
    ) {
        public PriceItem(Long goodsId, Long skuId, String goodsName, String specText, String image,
                         BigDecimal skuPrice, BigDecimal linePrice, Integer quantity, BigDecimal weight,
                         BigDecimal volume, List<Long> categoryIds) {
            this(goodsId, skuId, goodsName, specText, image, skuPrice, linePrice, quantity,
                    weight, volume, categoryIds, null, null);
        }

        public PriceItem(Long goodsId, Long skuId, String goodsName, String specText, String image,
                         BigDecimal skuPrice, BigDecimal linePrice, Integer quantity, BigDecimal weight, BigDecimal volume) {
            this(goodsId, skuId, goodsName, specText, image, skuPrice, linePrice, quantity,
                    weight, volume, null, null, null);
        }

        public PriceItem(Long goodsId, Long skuId, String goodsName, String specText, String image,
                         BigDecimal skuPrice, BigDecimal linePrice, Integer quantity, BigDecimal weight) {
            this(goodsId, skuId, goodsName, specText, image, skuPrice, linePrice, quantity,
                    weight, null, null, null, null);
        }
    }
}
