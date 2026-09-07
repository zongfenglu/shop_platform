package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.goods.entity.GoodsSpec;
import com.shopplatform.domain.goods.entity.GoodsSpecValue;
import com.shopplatform.domain.goods.service.GoodsSpecService;
import com.shopplatform.domain.goods.service.GoodsSpecValueService;
import com.shopplatform.storeapi.dto.CreateSpecRequest;
import com.shopplatform.storeapi.dto.CreateSpecValueRequest;
import com.shopplatform.storeapi.dto.SpecWithValues;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品规格管理（规格库，可跨商品复用，如"颜色"/"尺码"）。对应原型 store/goods-edit.html
 * 多规格区块的"规格：颜色/尺码"标签组 + "＋ 添加规格值"交互。
 * <p>
 * 这是此前 GoodsPublishView.vue 里多规格开关被禁用的唯一原因——GoodsSpecService/
 * GoodsSpecValueService 在 shop-domain 层早就写好了（TenantSafeService 骨架），
 * 只是没有 controller 暴露出来，前端拿不到数据。补上这个控制器后，多规格发布就有
 * 真实数据可用了（SKU 矩阵仍在 GoodsService#publishGoods 的 PublishGoodsCommand.skuItems 里，
 * 前端负责把选中的规格值组合枚举成 SKU 列表）。
 */
@RestController
@RequestMapping("/store/goods-specs")
public class StoreGoodsSpecController {

    private final GoodsSpecService goodsSpecService;
    private final GoodsSpecValueService goodsSpecValueService;

    public StoreGoodsSpecController(GoodsSpecService goodsSpecService, GoodsSpecValueService goodsSpecValueService) {
        this.goodsSpecService = goodsSpecService;
        this.goodsSpecValueService = goodsSpecValueService;
    }

    @GetMapping
    public Result<List<SpecWithValues>> list() {
        List<GoodsSpec> specs = goodsSpecService.list(Wrappers.<GoodsSpec>lambdaQuery().orderByAsc(GoodsSpec::getCreateTime));
        if (specs.isEmpty()) {
            return Result.ok(List.of());
        }

        List<Long> specIds = specs.stream().map(GoodsSpec::getId).toList();
        List<GoodsSpecValue> allValues = goodsSpecValueService.list(
                Wrappers.<GoodsSpecValue>lambdaQuery().in(GoodsSpecValue::getSpecId, specIds)
                        .orderByAsc(GoodsSpecValue::getCreateTime));
        Map<Long, List<GoodsSpecValue>> valuesBySpec = allValues.stream()
                .collect(Collectors.groupingBy(GoodsSpecValue::getSpecId));

        List<SpecWithValues> result = specs.stream()
                .map(spec -> new SpecWithValues(
                        spec.getId(),
                        spec.getName(),
                        valuesBySpec.getOrDefault(spec.getId(), List.of()).stream()
                                .map(v -> new SpecWithValues.GoodsSpecValueItem(v.getId(), v.getValue()))
                                .toList()))
                .toList();
        return Result.ok(result);
    }

    @PostMapping
    public Result<GoodsSpec> createSpec(@Valid @RequestBody CreateSpecRequest request) {
        GoodsSpec spec = new GoodsSpec();
        spec.setName(request.name());
        goodsSpecService.save(spec);
        return Result.ok(spec);
    }

    @PostMapping("/{specId}/values")
    public Result<GoodsSpecValue> createSpecValue(@PathVariable Long specId, @Valid @RequestBody CreateSpecValueRequest request) {
        // 校验规格属于当前租户——getByIdWithTenant 查不到会抛 404 语义的越权异常，
        // 避免 A 商户拿着 B 商户的 specId 往自己规格值表里插数据（同租户内部数据结构串联的越权点）。
        goodsSpecService.getByIdWithTenant(specId);

        GoodsSpecValue value = new GoodsSpecValue();
        value.setSpecId(specId);
        value.setValue(request.value());
        goodsSpecValueService.save(value);
        return Result.ok(value);
    }

    @DeleteMapping("/{specId}/values/{valueId}")
    public Result<Void> deleteSpecValue(@PathVariable Long specId, @PathVariable Long valueId) {
        goodsSpecService.getByIdWithTenant(specId);
        GoodsSpecValue value = goodsSpecValueService.getByIdWithTenant(valueId);
        if (!value.getSpecId().equals(specId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "规格值与规格不匹配");
        }
        goodsSpecValueService.removeById(valueId);
        return Result.ok();
    }
}
