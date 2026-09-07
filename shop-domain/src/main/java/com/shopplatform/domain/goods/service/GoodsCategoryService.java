package com.shopplatform.domain.goods.service;

import com.shopplatform.framework.mybatis.TenantSafeService;
import com.shopplatform.domain.goods.entity.GoodsCategory;

public interface GoodsCategoryService extends TenantSafeService<GoodsCategory> {

    int MAX_LEVEL = 3;

    record SaveCommand(Long parentId, String name, String image, Integer sort, Boolean isShow) {
    }

    GoodsCategory create(SaveCommand command);

    /** 更新名称/图片/排序/显隐，不改父级（避免把子树挪出三级限制）。 */
    GoodsCategory update(Long id, SaveCommand command);

    void deleteCategory(Long id);
}
