package com.shopplatform.clientapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.content.entity.Article;
import com.shopplatform.domain.content.service.ArticleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ConsumerArticleController {
    private final ArticleService articleService;
    public ConsumerArticleController(ArticleService articleService) { this.articleService = articleService; }

    @GetMapping
    public Result<IPage<Article>> list(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) Long categoryId) {
        return Result.ok(articleService.pageVisible(pageNum, Math.min(pageSize, 50), categoryId));
    }

    @GetMapping("/{id}")
    public Result<Article> detail(@PathVariable Long id) {
        return Result.ok(articleService.getVisibleAndIncreaseViews(id));
    }
}
