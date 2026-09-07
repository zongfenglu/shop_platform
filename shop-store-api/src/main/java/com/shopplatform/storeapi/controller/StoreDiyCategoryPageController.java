package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.diy.entity.ShopCategoryPage;
import com.shopplatform.domain.diy.service.ShopCategoryPageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类页模板。对应原型 store/diy-page-list.html「分类页样式」，
 * 全局唯一，不走 DIY 页面草稿/发布。
 */
@RestController
@RequestMapping("/store/diy/category-page")
public class StoreDiyCategoryPageController {

    private final ShopCategoryPageService shopCategoryPageService;

    public StoreDiyCategoryPageController(ShopCategoryPageService shopCategoryPageService) {
        this.shopCategoryPageService = shopCategoryPageService;
    }

    @GetMapping
    public Result<ShopCategoryPage> get() {
        return Result.ok(shopCategoryPageService.getOrCreate());
    }

    @PutMapping
    public Result<ShopCategoryPage> save(@Valid @RequestBody SaveRequest request) {
        return Result.ok(shopCategoryPageService.saveStyle(request.style(), request.shareTitle()));
    }

    public record SaveRequest(
            @NotBlank String style,
            @Size(max = 64) String shareTitle
    ) {
    }
}
