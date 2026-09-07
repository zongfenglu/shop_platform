package com.shopplatform.domain.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/** 用户-角色关联。 */
@TableName("platform_user_role")
public class PlatformUserRole extends BaseEntity {

    private Long userId;
    private Long roleId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
