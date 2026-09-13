package com.shopplatform.domain.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.domain.content.entity.Article;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface ArticleService extends TenantSafeService<Article> {
    record SaveCommand(Long categoryId, String title, String displayMode, String coverUrl,
                       String content, Integer virtualViews, String status, Integer sortNo) {}
    IPage<Article> pageMine(int pageNum, int pageSize, Long categoryId, String status, String keyword);
    Article create(SaveCommand command);
    Article update(Long id, SaveCommand command);
    void deleteArticle(Long id);
    IPage<Article> pageVisible(int pageNum, int pageSize, Long categoryId);
    Article getVisibleAndIncreaseViews(Long id);
}
