package com.shopplatform.domain.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 商户角色（租户内RBAC）。见文档三 §3.6、文档二 §2.12。
 */
@TableName("store_role")
public class StoreRole extends BaseEntity {

    private Long shopId;

    private String name;

    /** JSON：菜单权限点ID集合 */
    private String menuIds;

    /** all全部数据 / store仅本门店（门店店员角色用） */
    private String dataScope;

    private Boolean isBuiltin;

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

    public String getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(String menuIds) {
        this.menuIds = menuIds;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public Boolean getIsBuiltin() {
        return isBuiltin;
    }

    public void setIsBuiltin(Boolean isBuiltin) {
        this.isBuiltin = isBuiltin;
    }
}
