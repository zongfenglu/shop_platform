package com.shopplatform.domain.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 发货包裹。见 V9__order_package.sql 注释：一个订单可以对应多条记录（部分发货/多包裹场景）。
 */
@TableName("order_package")
public class OrderPackage extends BaseEntity {

    private Long shopId;

    private Long orderId;

    private String expressCompany;

    private String expressNo;

    /** JSON数组：本包裹包含的 order_goods.id 列表 */
    private String orderGoodsIds;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getExpressCompany() {
        return expressCompany;
    }

    public void setExpressCompany(String expressCompany) {
        this.expressCompany = expressCompany;
    }

    public String getExpressNo() {
        return expressNo;
    }

    public void setExpressNo(String expressNo) {
        this.expressNo = expressNo;
    }

    public String getOrderGoodsIds() {
        return orderGoodsIds;
    }

    public void setOrderGoodsIds(String orderGoodsIds) {
        this.orderGoodsIds = orderGoodsIds;
    }
}
