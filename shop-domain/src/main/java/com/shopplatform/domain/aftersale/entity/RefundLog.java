package com.shopplatform.domain.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * 退款调用流水。见 V10 迁移注释：一条 {@link AfterSale} 可能对应多次退款调用尝试（失败重试），
 * 流水表独立记录每次调用结果，方便排查，不覆盖售后单本身的最终状态字段。
 */
@TableName("refund_log")
public class RefundLog extends BaseEntity {

    private Long shopId;

    private Long afterSaleId;

    private String refundNo;

    private BigDecimal amount;

    /** success/closed/processing/abnormal/failed */
    private String status;

    private String rawResponse;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getAfterSaleId() {
        return afterSaleId;
    }

    public void setAfterSaleId(Long afterSaleId) {
        this.afterSaleId = afterSaleId;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
