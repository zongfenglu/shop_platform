package com.shopplatform.domain.diy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 行业模板库（平台级，无 shop_id），商户"一键套用"生成自己的 {@link DiyPage}。
 * 属于 {@link com.shopplatform.framework.mybatis.ShopTenantLineHandler} 的忽略表白名单。
 */
@TableName("diy_template")
public class DiyTemplate extends BaseEntity {

    /** 对应 shop.industry */
    private String industry;
    private String name;
    private String cover;
    private String pageData;
    private Boolean isShow;
    private Integer sort;

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }
    public String getPageData() { return pageData; }
    public void setPageData(String pageData) { this.pageData = pageData; }
    public Boolean getIsShow() { return isShow; }
    public void setIsShow(Boolean isShow) { this.isShow = isShow; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
