package com.shopplatform.domain.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 规格值（如"红色"）。见文档三 §3.2。
 */
@TableName("goods_spec_value")
public class GoodsSpecValue extends BaseEntity {

    private Long shopId;

    private Long specId;

    private String value;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getSpecId() {
        return specId;
    }

    public void setSpecId(Long specId) {
        this.specId = specId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
