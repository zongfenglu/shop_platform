package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.entity.DiyMaterialGroup;
import com.shopplatform.domain.diy.mapper.DiyMaterialGroupMapper;
import com.shopplatform.domain.diy.service.DiyMaterialGroupService;
import com.shopplatform.domain.diy.service.DiyMaterialService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DiyMaterialGroupServiceImpl extends ServiceImpl<DiyMaterialGroupMapper, DiyMaterialGroup>
        implements DiyMaterialGroupService {

    private static final int NAME_MAX = 16;
    private static final int GROUP_MAX = 50;

    private final DiyMaterialService diyMaterialService;

    public DiyMaterialGroupServiceImpl(@Lazy DiyMaterialService diyMaterialService) {
        this.diyMaterialService = diyMaterialService;
    }

    @Override
    public List<DiyMaterialGroup> listMine() {
        return this.list(Wrappers.<DiyMaterialGroup>lambdaQuery()
                .eq(DiyMaterialGroup::getShopId, TenantContext.getRequired())
                .orderByAsc(DiyMaterialGroup::getSort)
                .orderByAsc(DiyMaterialGroup::getCreateTime));
    }

    @Override
    public DiyMaterialGroup create(String name) {
        String trimmed = normalizeName(name);
        if (this.count() >= GROUP_MAX) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分组最多 " + GROUP_MAX + " 个");
        }
        assertNameUnique(trimmed, null);
        DiyMaterialGroup group = new DiyMaterialGroup();
        group.setShopId(TenantContext.getRequired());
        group.setName(trimmed);
        group.setSort(0);
        this.save(group);
        return group;
    }

    @Override
    public void rename(Long id, String name) {
        DiyMaterialGroup group = this.getByIdWithTenant(id);
        String trimmed = normalizeName(name);
        assertNameUnique(trimmed, id);
        group.setName(trimmed);
        this.updateById(group);
    }

    @Override
    @Transactional
    public void removeGroup(Long id) {
        this.getByIdWithTenant(id);
        diyMaterialService.clearGroup(id);
        this.removeById(id);
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请填写分组名称");
        }
        if (trimmed.length() > NAME_MAX) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分组名称不超过 " + NAME_MAX + " 个字");
        }
        return trimmed;
    }

    private void assertNameUnique(String name, Long excludeId) {
        long exists = this.count(Wrappers.<DiyMaterialGroup>lambdaQuery()
                .eq(DiyMaterialGroup::getName, name)
                .ne(excludeId != null, DiyMaterialGroup::getId, excludeId));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "已有同名分组");
        }
    }
}
