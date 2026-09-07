package com.shopplatform.domain.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * 会员等级。每个租户自配，建店时由 ShopServiceImpl 灌入默认等级。
 * 等级升级按 growth_value 匹配：取满足 growth_value &lt;= 会员成长值 中 weight 最大的等级。
 */
@TableName("user_grade")
public class UserGrade extends BaseEntity {

    private Long shopId;

    private String name;

    /** 排序权重，越大等级越高 */
    private Integer weight;

    /** 达到该等级所需成长值 */
    private Integer growthValue;

    /** 会员折扣 1.00=无折扣 0.90=九折 */
    private BigDecimal discountRatio;

    private String icon;

    private String remark;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getGrowthValue() {
        return growthValue;
    }

    public void setGrowthValue(Integer growthValue) {
        this.growthValue = growthValue;
    }

    public BigDecimal getDiscountRatio() {
        return discountRatio;
    }

    public void setDiscountRatio(BigDecimal discountRatio) {
        this.discountRatio = discountRatio;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
