package com.shopplatform.domain.dealer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/** 分销设置。每租户一条，StoreDealerSettingController GET/POST upsert。 */
@TableName("dealer_setting")
public class DealerSetting extends BaseEntity {

    private Long shopId;
    /** 0=disabled 1=enabled */
    private Integer isEnable;
    /** 佣金比例（百分比，如 10.00 = 10%） */
    private BigDecimal commissionRate;
    /** order(整单) / goods(按商品) */
    private String commissionType;
    /** 最低提现金额 */
    private BigDecimal minWithdraw;
    /** 0=人工审核 1=自动通过 */
    private Integer autoApprove;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Integer getIsEnable() { return isEnable; }
    public void setIsEnable(Integer isEnable) { this.isEnable = isEnable; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public String getCommissionType() { return commissionType; }
    public void setCommissionType(String commissionType) { this.commissionType = commissionType; }
    public BigDecimal getMinWithdraw() { return minWithdraw; }
    public void setMinWithdraw(BigDecimal minWithdraw) { this.minWithdraw = minWithdraw; }
    public Integer getAutoApprove() { return autoApprove; }
    public void setAutoApprove(Integer autoApprove) { this.autoApprove = autoApprove; }
}
