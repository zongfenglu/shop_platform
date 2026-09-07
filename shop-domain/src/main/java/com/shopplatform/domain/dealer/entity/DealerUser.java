package com.shopplatform.domain.dealer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 用户分销商资料。uk_shop_user 保证每个用户在一个租户下只有一条记录。 */
@TableName("dealer_user")
public class DealerUser extends BaseEntity {

    private Long shopId;
    private Long userId;
    /** 上级分销商（推荐人），形成推荐关系树 */
    private Long parentId;
    /** applying/active/disabled/rejected */
    private String status;
    private String realName;
    private String mobile;
    /** 累计佣金 */
    private BigDecimal totalCommission;
    /** 可提现佣金 */
    private BigDecimal availableCommission;
    /** 冻结佣金（售后期内） */
    private BigDecimal frozenCommission;
    /** 申请时间 */
    private LocalDateTime applyTime;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public BigDecimal getTotalCommission() { return totalCommission; }
    public void setTotalCommission(BigDecimal totalCommission) { this.totalCommission = totalCommission; }
    public BigDecimal getAvailableCommission() { return availableCommission; }
    public void setAvailableCommission(BigDecimal availableCommission) { this.availableCommission = availableCommission; }
    public BigDecimal getFrozenCommission() { return frozenCommission; }
    public void setFrozenCommission(BigDecimal frozenCommission) { this.frozenCommission = frozenCommission; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
}
