package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.goods.entity.GoodsCategory;
import com.shopplatform.domain.goods.service.GoodsCategoryService;
import com.shopplatform.storeapi.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类管理（三级树、图标、排序、显隐）。文档二 §2.2。
 */
@RestController
@RequestMapping("/store/goods-categories")
public class StoreGoodsCategoryController {

    private final GoodsCategoryService goodsCategoryService;

    public StoreGoodsCategoryController(GoodsCategoryService goodsCategoryService) {
        this.goodsCategoryService = goodsCategoryService;
    }

    @GetMapping
    public Result<List<GoodsCategory>> list() {
        return Result.ok(goodsCategoryService.list(
                Wrappers.<GoodsCategory>lambdaQuery()
                        .orderByAsc(GoodsCategory::getSort)
                        .orderByAsc(GoodsCategory::getId)));
    }

    @PostMapping
    public Result<GoodsCategory> create(@Valid @RequestBody CreateCategoryRequest request) {
        return Result.ok(goodsCategoryService.create(toCommand(request)));
    }

    @PutMapping("/{id}")
    public Result<GoodsCategory> update(@PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request) {
        return Result.ok(goodsCategoryService.update(id, toCommand(request)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsCategoryService.deleteCategory(id);
        return Result.ok();
    }

    private static GoodsCategoryService.SaveCommand toCommand(CreateCategoryRequest request) {
        return new GoodsCategoryService.SaveCommand(
                request.parentId(), request.name(), request.image(), request.sort(), request.isShow());
    }
}
