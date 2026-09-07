package com.shopplatform.domain.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 规格库（规格名，如"颜色"，可跨商品复用）。见文档三 §3.2。
 */
@TableName("goods_spec")
public class GoodsSpec extends BaseEntity {

    private Long shopId;

    private String name;

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
}
