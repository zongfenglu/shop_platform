package com.shopplatform.domain.content.service;

import com.shopplatform.domain.content.entity.ArticleCategory;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface ArticleCategoryService extends TenantSafeService<ArticleCategory> {
    record SaveCommand(String name, Integer sortNo, Boolean isShow) {}
    List<ArticleCategory> listMine();
    ArticleCategory create(SaveCommand command);
    ArticleCategory update(Long id, SaveCommand command);
    void deleteCategory(Long id);
}
