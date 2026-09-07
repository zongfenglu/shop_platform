package com.shopplatform.domain.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/** 角色-菜单关联。 */
@TableName("platform_role_menu")
public class PlatformRoleMenu extends BaseEntity {

    private Long roleId;
    private Long menuId;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }
}
