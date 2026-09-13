package com.shopplatform.domain.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

@TableName("content_article_category")
public class ArticleCategory extends BaseEntity {
    private Long shopId;
    private String name;
    private Integer sortNo;
    private Boolean isShow;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Boolean getIsShow() { return isShow; }
    public void setIsShow(Boolean show) { isShow = show; }
}
