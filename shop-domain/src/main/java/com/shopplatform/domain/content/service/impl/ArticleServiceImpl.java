package com.shopplatform.domain.content.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.content.entity.Article;
import com.shopplatform.domain.content.entity.ArticleCategory;
import com.shopplatform.domain.content.mapper.ArticleMapper;
import com.shopplatform.domain.content.service.ArticleCategoryService;
import com.shopplatform.domain.content.service.ArticleService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    private final ArticleCategoryService categoryService;

    public ArticleServiceImpl(ArticleCategoryService categoryService) { this.categoryService = categoryService; }

    @Override
    public IPage<Article> pageMine(int pageNum, int pageSize, Long categoryId, String status, String keyword) {
        var query = Wrappers.<Article>lambdaQuery().eq(Article::getShopId, TenantContext.getRequired());
        if (categoryId != null) query.eq(Article::getCategoryId, categoryId);
        if (StringUtils.hasText(status)) query.eq(Article::getStatus, status);
        if (StringUtils.hasText(keyword)) query.like(Article::getTitle, keyword.trim());
        query.select(Article.class, field -> !"content".equals(field.getColumn()));
        query.orderByAsc(Article::getSortNo).orderByDesc(Article::getCreateTime);
        return page(new Page<>(pageNum, pageSize), query);
    }

    @Override public Article create(SaveCommand command) {
        Article row = new Article();
        row.setShopId(TenantContext.getRequired());
        row.setActualViews(0);
        apply(row, command);
        save(row);
        return row;
    }

    @Override public Article update(Long id, SaveCommand command) {
        Article row = getByIdWithTenant(id);
        apply(row, command);
        updateById(row);
        return row;
    }

    @Override public void deleteArticle(Long id) { removeById(getByIdWithTenant(id).getId()); }

    @Override
    public IPage<Article> pageVisible(int pageNum, int pageSize, Long categoryId) {
        var query = Wrappers.<Article>lambdaQuery()
                .eq(Article::getShopId, TenantContext.getRequired())
                .eq(Article::getStatus, "visible");
        if (categoryId != null) query.eq(Article::getCategoryId, categoryId);
        query.select(Article.class, field -> !"content".equals(field.getColumn()));
        query.orderByAsc(Article::getSortNo).orderByDesc(Article::getCreateTime);
        return page(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public Article getVisibleAndIncreaseViews(Long id) {
        Article row = getOne(Wrappers.<Article>lambdaQuery()
                .eq(Article::getId, id).eq(Article::getStatus, "visible"), false);
        if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在或已隐藏");
        update(Wrappers.<Article>lambdaUpdate().eq(Article::getId, id)
                .setSql("actual_views = actual_views + 1"));
        row.setActualViews((row.getActualViews() == null ? 0 : row.getActualViews()) + 1);
        return row;
    }

    private void apply(Article row, SaveCommand command) {
        ArticleCategory category = categoryService.getByIdWithTenant(command.categoryId());
        String title = command.title() == null ? "" : command.title().trim();
        if (!StringUtils.hasText(title)) throw new BusinessException(ErrorCode.PARAM_INVALID, "请输入文章标题");
        if (title.length() > 120) throw new BusinessException(ErrorCode.PARAM_INVALID, "文章标题不能超过120字");
        if (!List.of("small", "large").contains(command.displayMode()))
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文章列表显示方式不正确");
        if (!List.of("visible", "hidden").contains(command.status()))
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文章状态不正确");
        if (!StringUtils.hasText(command.coverUrl())) throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择文章封面图");
        if (!StringUtils.hasText(command.content())) throw new BusinessException(ErrorCode.PARAM_INVALID, "请输入文章内容");
        row.setCategoryId(category.getId());
        row.setTitle(title);
        row.setDisplayMode(command.displayMode());
        row.setCoverUrl(command.coverUrl().trim());
        row.setContent(command.content());
        row.setVirtualViews(Math.max(0, command.virtualViews() == null ? 0 : command.virtualViews()));
        row.setStatus(command.status());
        row.setSortNo(command.sortNo() == null ? 0 : command.sortNo());
    }
}
