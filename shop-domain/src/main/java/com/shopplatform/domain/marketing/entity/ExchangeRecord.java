package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * 积分兑换记录。goods_type=coupon 发券（即时），=goods 发货（待发货队列）。
 * create 仅校验不扣减，pay 事务内先扣库存再扣积分，镜像 recharge_order 两段式。
 */
@TableName("exchange_record")
public class ExchangeRecord extends BaseEntity {

    private Long shopId;
    private Long userId;
    private Long pointsGoodsId;
    /** coupon发券 / goods发货 */
    private String goodsType;
    private Long couponId;
    /** goods_type=coupon 时发出的 user_coupon.id */
    private Long userCouponId;
    /** 扣减的积分 */
    private Integer pointsCost;
    /** 现金部分，0=纯积分 */
    private BigDecimal cashPrice;
    /** unpaid / paid */
    private String payStatus;
    /** pending待发货 / fulfilled已发货（goods类） */
    private String status;
    /** 兑换时名称快照 */
    private String name;
    /** 兑换时图片快照 */
    private String image;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPointsGoodsId() { return pointsGoodsId; }
    public void setPointsGoodsId(Long pointsGoodsId) { this.pointsGoodsId = pointsGoodsId; }
    public String getGoodsType() { return goodsType; }
    public void setGoodsType(String goodsType) { this.goodsType = goodsType; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public Long getUserCouponId() { return userCouponId; }
    public void setUserCouponId(Long userCouponId) { this.userCouponId = userCouponId; }
    public Integer getPointsCost() { return pointsCost; }
    public void setPointsCost(Integer pointsCost) { this.pointsCost = pointsCost; }
    public BigDecimal getCashPrice() { return cashPrice; }
    public void setCashPrice(BigDecimal cashPrice) { this.cashPrice = cashPrice; }
    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
