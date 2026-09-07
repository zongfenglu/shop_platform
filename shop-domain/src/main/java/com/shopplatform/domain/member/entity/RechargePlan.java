package com.shopplatform.domain.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * 余额充值方案。用户实付 money，到账 money+gift_money，另送 gift_points 积分。
 */
@TableName("recharge_plan")
public class RechargePlan extends BaseEntity {

    private Long shopId;

    /** 用户实付金额 */
    private BigDecimal money;

    /** 赠送余额 */
    private BigDecimal giftMoney;

    /** 赠送积分 */
    private Integer giftPoints;

    private Integer isShow;

    private Integer sort;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public BigDecimal getGiftMoney() {
        return giftMoney;
    }

    public void setGiftMoney(BigDecimal giftMoney) {
        this.giftMoney = giftMoney;
    }

    public Integer getGiftPoints() {
        return giftPoints;
    }

    public void setGiftPoints(Integer giftPoints) {
        this.giftPoints = giftPoints;
    }

    public Integer getIsShow() {
        return isShow;
    }

    public void setIsShow(Integer isShow) {
        this.isShow = isShow;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
