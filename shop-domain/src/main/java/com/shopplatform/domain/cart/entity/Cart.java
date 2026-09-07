package com.shopplatform.domain.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 购物车行，按 SKU 维度（见 V11 迁移注释）：同一商品的不同规格是两条独立记录，
 * 与下单链路"只认 SKU、不感知单/多规格"的约定保持一致。
 * <p>
 * 刻意不存价格快照——展示价一律实时读 {@code goods_sku.price}，
 * 否则商家改价后购物车长期显示旧价、结算时又跳成新价。
 */
@TableName("cart")
public class Cart extends BaseEntity {

    private Long shopId;

    private Long userId;

    private Long goodsId;

    private Long skuId;

    private Integer quantity;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
