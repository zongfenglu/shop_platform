package com.shopplatform.domain.diy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 商户素材库条目。记录一次成功的图片上传，装修编辑器可从素材库复用已上传的图。
 * 删除只软删这条记录，不删物理文件——已发布的页面可能还在引用同一个 URL。
 */
@TableName("diy_material")
public class DiyMaterial extends BaseEntity {

    private Long shopId;
    private String url;
    private String name;
    private Long size;
    /** 所属分组，空=未分组 */
    private Long groupId;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
}
