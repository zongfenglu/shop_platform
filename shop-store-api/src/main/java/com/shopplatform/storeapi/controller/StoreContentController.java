package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.content.entity.Article;
import com.shopplatform.domain.content.entity.ArticleCategory;
import com.shopplatform.domain.content.service.ArticleCategoryService;
import com.shopplatform.domain.content.service.ArticleService;
import com.shopplatform.storeapi.dto.SaveArticleCategoryRequest;
import com.shopplatform.storeapi.dto.SaveArticleRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store/content")
public class StoreContentController {
    private final ArticleCategoryService categoryService;
    private final ArticleService articleService;

    public StoreContentController(ArticleCategoryService categoryService, ArticleService articleService) {
        this.categoryService = categoryService;
        this.articleService = articleService;
    }

    @GetMapping("/article-categories")
    public Result<List<ArticleCategory>> categories() { return Result.ok(categoryService.listMine()); }
    @PostMapping("/article-categories")
    public Result<ArticleCategory> createCategory(@Valid @RequestBody SaveArticleCategoryRequest r) {
        return Result.ok(categoryService.create(new ArticleCategoryService.SaveCommand(r.name(), r.sortNo(), r.isShow())));
    }
    @PutMapping("/article-categories/{id}")
    public Result<ArticleCategory> updateCategory(@PathVariable Long id, @Valid @RequestBody SaveArticleCategoryRequest r) {
        return Result.ok(categoryService.update(id, new ArticleCategoryService.SaveCommand(r.name(), r.sortNo(), r.isShow())));
    }
    @DeleteMapping("/article-categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) { categoryService.deleteCategory(id); return Result.ok(); }

    @GetMapping("/articles")
    public Result<IPage<Article>> articles(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "20") int pageSize,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(articleService.pageMine(pageNum, Math.min(pageSize, 100), categoryId, status, keyword));
    }
    @GetMapping("/articles/{id}")
    public Result<Article> article(@PathVariable Long id) { return Result.ok(articleService.getByIdWithTenant(id)); }
    @PostMapping("/articles")
    public Result<Article> createArticle(@Valid @RequestBody SaveArticleRequest r) { return Result.ok(articleService.create(command(r))); }
    @PutMapping("/articles/{id}")
    public Result<Article> updateArticle(@PathVariable Long id, @Valid @RequestBody SaveArticleRequest r) { return Result.ok(articleService.update(id, command(r))); }
    @DeleteMapping("/articles/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) { articleService.deleteArticle(id); return Result.ok(); }

    private static ArticleService.SaveCommand command(SaveArticleRequest r) {
        return new ArticleService.SaveCommand(r.categoryId(), r.title(), r.displayMode(), r.coverUrl(),
                r.content(), r.virtualViews(), r.status(), r.sortNo());
    }
}
