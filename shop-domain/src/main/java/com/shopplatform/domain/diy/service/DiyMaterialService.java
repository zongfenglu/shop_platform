package com.shopplatform.domain.diy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.domain.diy.entity.DiyMaterial;
import com.shopplatform.domain.file.service.StorageService;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.Map;

public interface DiyMaterialService extends TenantSafeService<DiyMaterial> {

    /**
     * 当前商户素材分页。
     * @param groupId 指定分组；null 且 ungrouped=false 表示全部
     * @param ungrouped true 时只看未分组
     * @param type image / video；null 表示不过滤（老调用方默认只关心图片时应显式传 image）
     */
    IPage<DiyMaterial> pageMine(int pageNum, int pageSize, Long groupId, boolean ungrouped, String keyword, String type);

    DiyMaterial record(StorageService.StoredFile file, Long groupId, String type);

    void moveToGroup(Long id, Long groupId);

    /** 分组被删时，组内素材回落到未分组。 */
    void clearGroup(Long groupId);

    /** 按 groupId 计数；key=null 表示未分组。 */
    Map<Long, Long> countByGroup();

    /** 软删一条素材记录。物理文件保留——已发布页面可能仍在引用该 URL。 */
    void remove(Long id);
}
