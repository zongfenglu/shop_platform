package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * 积分商城兑换项。goods_id/coupon_id 二选一，分别对应实物发货和直接发券。
 * stock=0 表示不限量。
 */
@TableName("points_goods")
public class PointsGoods extends BaseEntity {

    private Long shopId;
    /** 展示名称 */
    private String name;
    /** 展示图 */
    private String image;
    /** 实物商品 goods.id（发货类） */
    private Long goodsId;
    /** 优惠券 coupon.id（发券类） */
    private Long couponId;
    /** 所需积分 */
    private Integer points;
    /** 积分+现金模式下的现金部分，0/NULL=纯积分 */
    private BigDecimal cash;
    /** 库存，0=不限 */
    private Integer stock;
    /** on / off */
    private String status;
    /** 排序，越小越靠前 */
    private Integer sort;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public BigDecimal getCash() { return cash; }
    public void setCash(BigDecimal cash) { this.cash = cash; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
