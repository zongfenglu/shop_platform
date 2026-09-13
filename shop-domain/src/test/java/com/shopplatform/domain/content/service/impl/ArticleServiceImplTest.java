package com.shopplatform.domain.content.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.content.entity.Article;
import com.shopplatform.domain.content.entity.ArticleCategory;
import com.shopplatform.domain.content.service.ArticleCategoryService;
import com.shopplatform.domain.content.service.ArticleService;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ArticleServiceImplTest {
    private ArticleCategoryService categoryService;
    private ArticleServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContext.set(1001L);
        categoryService = mock(ArticleCategoryService.class);
        ArticleCategory category = new ArticleCategory();
        category.setId(9L);
        category.setIsShow(true);
        when(categoryService.getByIdWithTenant(9L)).thenReturn(category);
        service = spy(new ArticleServiceImpl(categoryService));
    }

    @AfterEach void tearDown() { TenantContext.clear(); }

    @Test
    void create_setsTenantAndNormalizesNumbers() {
        doReturn(true).when(service).save(any(Article.class));
        Article row = service.create(command("small", "visible", -5));
        assertEquals(1001L, row.getShopId());
        assertEquals("文章标题", row.getTitle());
        assertEquals(0, row.getVirtualViews());
        assertEquals(0, row.getActualViews());
    }

    @Test
    void create_rejectsInvalidDisplayMode() {
        assertThrows(BusinessException.class, () -> service.create(command("card", "visible", 1)));
    }

    private ArticleService.SaveCommand command(String mode, String status, int views) {
        return new ArticleService.SaveCommand(9L, " 文章标题 ", mode, "/uploads/cover.jpg",
                "<p>正文</p>", views, status, 10);
    }
}
