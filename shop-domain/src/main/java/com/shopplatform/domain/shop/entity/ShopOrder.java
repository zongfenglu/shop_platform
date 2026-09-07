package com.shopplatform.domain.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城订购单（新开通/续费/升级/增值服务）。
 * 对应数据库表 {@code shop_order}，属于平台侧订单，不受租户行级隔离影响。
 */
@TableName("shop_order")
public class ShopOrder extends BaseEntity {

    private String orderNo;

    private Long shopId;

    /** new / renew / upgrade / addon */
    private String type;

    private Long packageTplId;

    private Integer durationMonth;

    private BigDecimal amount;

    /** pending / paid / refunded / closed */
    private String payStatus;

    /** wechat / alipay / offline */
    private String payMethod;

    private LocalDateTime payTime;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getPackageTplId() {
        return packageTplId;
    }

    public void setPackageTplId(Long packageTplId) {
        this.packageTplId = packageTplId;
    }

    public Integer getDurationMonth() {
        return durationMonth;
    }

    public void setDurationMonth(Integer durationMonth) {
        this.durationMonth = durationMonth;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }
}
