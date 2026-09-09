package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.storeapi.dto.GoodsListQuery;
import com.shopplatform.storeapi.dto.PublishGoodsRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品管理。对应原型 store/goods-list.html、goods-edit.html。
 */
@RestController
@RequestMapping("/store/goods")
public class StoreGoodsController {

    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;

    public StoreGoodsController(GoodsService goodsService, GoodsSkuService goodsSkuService) {
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
    }

    @GetMapping
    public Result<IPage<Goods>> list(GoodsListQuery query) {
        var wrapper = Wrappers.<Goods>lambdaQuery();
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(Goods::getName, query.keyword()).or().like(Goods::getCode, query.keyword()));
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(Goods::getStatus, query.status());
        }
        wrapper.orderByDesc(Goods::getCreateTime);

        Page<Goods> page = new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault());
        return Result.ok(goodsService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Goods goods = goodsService.getByIdWithTenant(id);
        List<GoodsSku> skus = goodsSkuService.listByGoodsId(id);
        return Result.ok(Map.of("goods", goods, "skus", skus));
    }

    @PostMapping
    public Result<Goods> publish(@Valid @RequestBody PublishGoodsRequest request) {
        Goods goods = goodsService.publishGoods(toCommand(request));
        return Result.ok(goods);
    }

    @PutMapping("/{id}")
    public Result<Goods> update(@PathVariable Long id, @Valid @RequestBody PublishGoodsRequest request) {
        return Result.ok(goodsService.updateGoods(id, toCommand(request)));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        if (!List.of("on", "off", "deleted").contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "非法的商品状态");
        }
        goodsService.updateStatus(id, status);
        return Result.ok();
    }

    private GoodsService.PublishGoodsCommand toCommand(PublishGoodsRequest request) {
        List<GoodsService.SkuItem> skuItems = request.skuItems().stream()
                .map(i -> new GoodsService.SkuItem(
                        i.specValueIds(), i.skuCode(), i.price(), i.linePrice(), i.costPrice(),
                        i.stock(), i.weight(), i.volume(), i.image()))
                .toList();
        return new GoodsService.PublishGoodsCommand(
                request.categoryIds(), request.brandId(), request.name(), request.subName(), request.code(),
                request.images(), request.specType(), request.content(), request.deliveryType(),
                request.freightTemplateId(), request.freightFee(), request.serviceIds(),
                request.isVirtual(), request.commissionRate(), skuItems);
    }
}
