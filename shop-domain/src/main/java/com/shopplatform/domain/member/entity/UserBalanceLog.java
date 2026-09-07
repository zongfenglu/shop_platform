package com.shopplatform.domain.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * 会员余额变动流水。money 带正负号：充值+/消费-/退款+/后台调整±/佣金+。
 * before/after 由 MemberService.adjustBalance 在原子更新成功后按新余额回算，保证与 user.balance 一致。
 */
@TableName("user_balance_log")
public class UserBalanceLog extends BaseEntity {

    private Long shopId;

    private Long userId;

    /** recharge/consume/refund/admin/commission */
    private String scene;

    private BigDecimal money;

    private BigDecimal before;

    private BigDecimal after;

    private String remark;

    private Long orderId;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public BigDecimal getBefore() {
        return before;
    }

    public void setBefore(BigDecimal before) {
        this.before = before;
    }

    public BigDecimal getAfter() {
        return after;
    }

    public void setAfter(BigDecimal after) {
        this.after = after;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
