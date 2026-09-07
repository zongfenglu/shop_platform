package com.shopplatform.domain.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/** 平台角色。is_builtin=1 为系统内置，不可编辑删除。 */
@TableName("platform_role")
public class PlatformRole extends BaseEntity {

    private String name;
    private String remark;
    private Integer isBuiltin;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getIsBuiltin() { return isBuiltin; }
    public void setIsBuiltin(Integer isBuiltin) { this.isBuiltin = isBuiltin; }
}
