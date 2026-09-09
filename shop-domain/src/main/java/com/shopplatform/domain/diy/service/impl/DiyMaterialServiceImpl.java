package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.diy.entity.DiyMaterial;
import com.shopplatform.domain.diy.mapper.DiyMaterialMapper;
import com.shopplatform.domain.diy.service.DiyMaterialGroupService;
import com.shopplatform.domain.diy.service.DiyMaterialService;
import com.shopplatform.domain.file.service.StorageService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiyMaterialServiceImpl extends ServiceImpl<DiyMaterialMapper, DiyMaterial> implements DiyMaterialService {

    private final DiyMaterialGroupService groupService;

    public DiyMaterialServiceImpl(@Lazy DiyMaterialGroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    public IPage<DiyMaterial> pageMine(int pageNum, int pageSize, Long groupId, boolean ungrouped, String keyword, String type) {
        var wrapper = Wrappers.<DiyMaterial>lambdaQuery()
                .eq(DiyMaterial::getShopId, TenantContext.getRequired())
                .orderByDesc(DiyMaterial::getCreateTime);
        if (ungrouped) {
            wrapper.isNull(DiyMaterial::getGroupId);
        } else if (groupId != null) {
            wrapper.eq(DiyMaterial::getGroupId, groupId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DiyMaterial::getName, keyword.trim());
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(DiyMaterial::getType, type);
        }
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public DiyMaterial record(StorageService.StoredFile file, Long groupId, String type) {
        DiyMaterial material = new DiyMaterial();
        material.setShopId(TenantContext.getRequired());
        material.setUrl(file.url());
        material.setType(StringUtils.hasText(type) ? type : "image");
        material.setName(file.name());
        material.setSize(file.size());
        material.setGroupId(resolveGroupId(groupId));
        this.save(material);
        return material;
    }

    @Override
    public void moveToGroup(Long id, Long groupId) {
        DiyMaterial material = this.getByIdWithTenant(id);
        material.setGroupId(resolveGroupId(groupId));
        this.updateById(material);
    }

    @Override
    public void clearGroup(Long groupId) {
        this.update(Wrappers.<DiyMaterial>lambdaUpdate()
                .eq(DiyMaterial::getShopId, TenantContext.getRequired())
                .eq(DiyMaterial::getGroupId, groupId)
                .set(DiyMaterial::getGroupId, null));
    }

    @Override
    public Map<Long, Long> countByGroup() {
        List<DiyMaterial> rows = this.list(Wrappers.<DiyMaterial>lambdaQuery()
                .eq(DiyMaterial::getShopId, TenantContext.getRequired())
                .select(DiyMaterial::getId, DiyMaterial::getGroupId));
        Map<Long, Long> map = new HashMap<>();
        for (DiyMaterial row : rows) {
            map.merge(row.getGroupId(), 1L, Long::sum);
        }
        return map;
    }

    @Override
    public void remove(Long id) {
        DiyMaterial material = this.getByIdWithTenant(id);
        this.removeById(material.getId());
    }

    private Long resolveGroupId(Long groupId) {
        if (groupId == null) {
            return null;
        }
        groupService.getByIdWithTenant(groupId);
        return groupId;
    }
}
