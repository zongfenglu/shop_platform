package com.shopplatform.domain.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

@TableName("content_article")
public class Article extends BaseEntity {
    private Long shopId;
    private Long categoryId;
    private String title;
    private String displayMode;
    private String coverUrl;
    private String content;
    private Integer virtualViews;
    private Integer actualViews;
    private String status;
    private Integer sortNo;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDisplayMode() { return displayMode; }
    public void setDisplayMode(String displayMode) { this.displayMode = displayMode; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getVirtualViews() { return virtualViews; }
    public void setVirtualViews(Integer virtualViews) { this.virtualViews = virtualViews; }
    public Integer getActualViews() { return actualViews; }
    public void setActualViews(Integer actualViews) { this.actualViews = actualViews; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}
