package com.shopplatform.domain.diy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 商户素材库分组。一张图只属于一个分组；未指定分组的素材 group_id 为空。
 */
@TableName("diy_material_group")
public class DiyMaterialGroup extends BaseEntity {

    private Long shopId;
    private String name;
    private Integer sort;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
