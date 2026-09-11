package com.shopplatform.domain.setting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

@TableName("store_express_company")
public class ExpressCompany extends BaseEntity {
    private Long shopId;
    private String name;
    private String code;
    private Integer sort;
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
