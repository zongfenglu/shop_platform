package com.shopplatform.domain.dealer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 提现申请。分销商申请提现 → Store 审核 → 通过/拒绝/已打款。 */
@TableName("dealer_withdraw")
public class DealerWithdraw extends BaseEntity {

    private Long shopId;
    private Long dealerUserId;
    private Long userId;
    /** 提现金额 */
    private BigDecimal amount;
    /** wechat/alipay/bank */
    private String method;
    /** 收款账户（脱敏） */
    private String accountInfo;
    /** applying/approved/rejected/paid */
    private String status;
    private String remark;
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getDealerUserId() { return dealerUserId; }
    public void setDealerUserId(Long dealerUserId) { this.dealerUserId = dealerUserId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getAccountInfo() { return accountInfo; }
    public void setAccountInfo(String accountInfo) { this.accountInfo = accountInfo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
}
