package com.shopplatform.domain.diy.service;

import com.shopplatform.domain.diy.entity.DiyMaterialGroup;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface DiyMaterialGroupService extends TenantSafeService<DiyMaterialGroup> {

    List<DiyMaterialGroup> listMine();

    DiyMaterialGroup create(String name);

    void rename(Long id, String name);

    /** 删除分组，组内素材回落到未分组，不删文件。 */
    void removeGroup(Long id);
}
