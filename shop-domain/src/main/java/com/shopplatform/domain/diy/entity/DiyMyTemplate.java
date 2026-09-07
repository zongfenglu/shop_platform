package com.shopplatform.domain.diy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 商户私有装修模板（带 shop_id，走正常租户隔离），与平台级只读 {@link DiyTemplate} 语义分离。
 */
@TableName("diy_my_template")
public class DiyMyTemplate extends BaseEntity {

    private Long shopId;
    private String name;
    private String pageData;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPageData() { return pageData; }
    public void setPageData(String pageData) { this.pageData = pageData; }
}
