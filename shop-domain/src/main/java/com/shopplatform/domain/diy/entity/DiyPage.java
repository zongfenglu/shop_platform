package com.shopplatform.domain.diy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/** 装修页面（草稿/发布双版本）。Sprint 14。 */
@TableName("diy_page")
public class DiyPage extends BaseEntity {

    private Long shopId;
    /** home/custom/category/user */
    private String pageType;
    private String name;
    /** 已发布内容快照，从未发布过则为null */
    private String pageData;
    /** 草稿内容 */
    private String draftData;
    /** 是否为当前店铺首页；设为首页时会同时把 pageType 写成 home */
    private Boolean isDefault;
    private Integer version;
    private LocalDateTime publishTime;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getPageType() { return pageType; }
    public void setPageType(String pageType) { this.pageType = pageType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPageData() { return pageData; }
    public void setPageData(String pageData) { this.pageData = pageData; }
    public String getDraftData() { return draftData; }
    public void setDraftData(String draftData) { this.draftData = draftData; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
}
