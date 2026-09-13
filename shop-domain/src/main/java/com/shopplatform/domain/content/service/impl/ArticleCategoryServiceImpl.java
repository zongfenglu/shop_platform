package com.shopplatform.domain.content.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.content.entity.Article;
import com.shopplatform.domain.content.entity.ArticleCategory;
import com.shopplatform.domain.content.mapper.ArticleCategoryMapper;
import com.shopplatform.domain.content.mapper.ArticleMapper;
import com.shopplatform.domain.content.service.ArticleCategoryService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ArticleCategoryServiceImpl extends ServiceImpl<ArticleCategoryMapper, ArticleCategory>
        implements ArticleCategoryService {
    private final ArticleMapper articleMapper;

    public ArticleCategoryServiceImpl(ArticleMapper articleMapper) { this.articleMapper = articleMapper; }

    @Override
    public List<ArticleCategory> listMine() {
        return list(Wrappers.<ArticleCategory>lambdaQuery()
                .eq(ArticleCategory::getShopId, TenantContext.getRequired())
                .orderByAsc(ArticleCategory::getSortNo).orderByAsc(ArticleCategory::getId));
    }

    @Override
    public ArticleCategory create(SaveCommand command) {
        ArticleCategory row = new ArticleCategory();
        row.setShopId(TenantContext.getRequired());
        apply(row, command);
        save(row);
        return row;
    }

    @Override
    public ArticleCategory update(Long id, SaveCommand command) {
        ArticleCategory row = getByIdWithTenant(id);
        apply(row, command);
        updateById(row);
        return row;
    }

    @Override
    public void deleteCategory(Long id) {
        getByIdWithTenant(id);
        if (articleMapper.selectCount(Wrappers.<Article>lambdaQuery().eq(Article::getCategoryId, id)) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分类下仍有文章，不能删除");
        }
        removeById(id);
    }

    private void apply(ArticleCategory row, SaveCommand command) {
        String name = command.name() == null ? "" : command.name().trim();
        if (!StringUtils.hasText(name)) throw new BusinessException(ErrorCode.PARAM_INVALID, "请输入分类名称");
        if (name.length() > 64) throw new BusinessException(ErrorCode.PARAM_INVALID, "分类名称不能超过64字");
        row.setName(name);
        row.setSortNo(command.sortNo() == null ? 0 : command.sortNo());
        row.setIsShow(command.isShow() == null || command.isShow());
    }
}
