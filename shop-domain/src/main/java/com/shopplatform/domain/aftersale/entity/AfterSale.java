package com.shopplatform.domain.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后单。见文档三 §3.3、开发计划 Sprint 6：
 * refundAmount/refundDetail 由 {@link com.shopplatform.domain.aftersale.RefundCalculator} 按
 * {@code order_goods.discount_detail} 记录的分摊比例逆向计算，不重新走价格引擎——
 * 退款金额的口径必须与下单时的分摊口径完全一致，否则会出现"当时优惠5块，退款却退了8块"的账不平问题。
 */
@TableName("after_sale")
public class AfterSale extends BaseEntity {

    private Long shopId;

    private Long orderId;

    private Long orderGoodsId;

    private Long userId;

    /** refund_only仅退款 / return_refund退货退款 */
    private String type;

    private String applyReason;

    private String applyDesc;

    /** JSON数组：凭证图片 */
    private String images;

    private Integer refundNum;

    private BigDecimal refundAmount;

    /** JSON：退款分摊明细，结构与 order_goods.discount_detail 对应 */
    private String refundDetail;

    /** applying/approved/rejected/return_shipped/refunding/refunded/closed */
    private String status;

    private String auditRemark;

    private String returnExpressCompany;

    private String returnExpressNo;

    private String refundNo;

    private String wxRefundId;

    private LocalDateTime refundTime;

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

    public Long getOrderGoodsId() {
        return orderGoodsId;
    }

    public void setOrderGoodsId(Long orderGoodsId) {
        this.orderGoodsId = orderGoodsId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApplyReason() {
        return applyReason;
    }

    public void setApplyReason(String applyReason) {
        this.applyReason = applyReason;
    }

    public String getApplyDesc() {
        return applyDesc;
    }

    public void setApplyDesc(String applyDesc) {
        this.applyDesc = applyDesc;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getRefundNum() {
        return refundNum;
    }

    public void setRefundNum(Integer refundNum) {
        this.refundNum = refundNum;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundDetail() {
        return refundDetail;
    }

    public void setRefundDetail(String refundDetail) {
        this.refundDetail = refundDetail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    public String getReturnExpressCompany() {
        return returnExpressCompany;
    }

    public void setReturnExpressCompany(String returnExpressCompany) {
        this.returnExpressCompany = returnExpressCompany;
    }

    public String getReturnExpressNo() {
        return returnExpressNo;
    }

    public void setReturnExpressNo(String returnExpressNo) {
        this.returnExpressNo = returnExpressNo;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public String getWxRefundId() {
        return wxRefundId;
    }

    public void setWxRefundId(String wxRefundId) {
        this.wxRefundId = wxRefundId;
    }

    public LocalDateTime getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(LocalDateTime refundTime) {
        this.refundTime = refundTime;
    }
}
