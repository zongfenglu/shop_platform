package com.shopplatform.domain.dealer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 佣金记录。下单支付时创建，状态 pending→settled（售后期满后）或 refunded（退款时回退）。 */
@TableName("dealer_order")
public class DealerOrder extends BaseEntity {

    private Long shopId;
    private Long orderId;
    private Long dealerUserId;
    /** 订单金额（快照） */
    private BigDecimal orderTotal;
    /** 佣金比例（快照） */
    private BigDecimal commissionRate;
    /** 佣金金额 */
    private BigDecimal commissionAmount;
    /** pending/settled/refunded */
    private String status;
    /** 结算时间（售后期满转可提现） */
    private LocalDateTime settleTime;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getDealerUserId() { return dealerUserId; }
    public void setDealerUserId(Long dealerUserId) { this.dealerUserId = dealerUserId; }
    public BigDecimal getOrderTotal() { return orderTotal; }
    public void setOrderTotal(BigDecimal orderTotal) { this.orderTotal = orderTotal; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSettleTime() { return settleTime; }
    public void setSettleTime(LocalDateTime settleTime) { this.settleTime = settleTime; }
}
