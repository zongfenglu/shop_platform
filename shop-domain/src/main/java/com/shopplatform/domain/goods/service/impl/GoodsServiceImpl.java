package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.mapper.GoodsMapper;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.shop.service.PackageQuotaChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    private final GoodsSkuService goodsSkuService;
    private final ObjectMapper objectMapper;
    private final PackageQuotaChecker packageQuotaChecker;

    public GoodsServiceImpl(GoodsSkuService goodsSkuService,
                            ObjectMapper objectMapper,
                            PackageQuotaChecker packageQuotaChecker) {
        this.goodsSkuService = goodsSkuService;
        this.objectMapper = objectMapper;
        this.packageQuotaChecker = packageQuotaChecker;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Goods publishGoods(PublishGoodsCommand cmd) {
        packageQuotaChecker.requireGoods();
        List<SkuItem> skuItems = normalizeSkuItems(cmd);
        validateSkuItems(skuItems, cmd.specType());

        Goods goods = new Goods();
        goods.setCategoryIds(toJson(cmd.categoryIds()));
        goods.setBrandId(cmd.brandId());
        goods.setName(cmd.name());
        goods.setSubName(cmd.subName());
        goods.setCode(cmd.code());
        goods.setImages(toJson(cmd.images()));
        goods.setSpecType(resolveSpecType(skuItems, cmd.specType()));
        goods.setContent(cmd.content());
        goods.setStatus("off");
        goods.setSalesInitial(0);
        goods.setSalesActual(0);
        goods.setDeliveryType(toJson(cmd.deliveryType()));
        goods.setFreightTemplateId(cmd.freightTemplateId());
        goods.setFreightFee(cmd.freightFee());
        goods.setServiceIds(toJson(cmd.serviceIds()));
        goods.setIsVirtual(Boolean.TRUE.equals(cmd.isVirtual()));
        goods.setLimitType("none");
        goods.setSort(0);

        int stockTotal = skuItems.stream().mapToInt(i -> Optional.ofNullable(i.stock()).orElse(0)).sum();
        goods.setStockTotal(stockTotal);

        this.save(goods);

        // 核心约束（文档三 §3.2）：无论单规格还是多规格，都统一落地为 goods_sku 记录，
        // 单规格商品固定生成一条 spec_value_ids='' 的 SKU——不为单规格另写分支。
        for (SkuItem item : skuItems) {
            GoodsSku sku = new GoodsSku();
            sku.setGoodsId(goods.getId());
            sku.setSkuCode(item.skuCode());
            sku.setSpecValueIds(normalizeSpecValueIds(item.specValueIds()));
            sku.setPrice(item.price());
            sku.setLinePrice(item.linePrice());
            sku.setCostPrice(item.costPrice());
            sku.setStock(Optional.ofNullable(item.stock()).orElse(0));
            sku.setWeight(item.weight());
            sku.setVolume(item.volume());
            sku.setImage(item.image());
            goodsSkuService.save(sku);
        }

        return goods;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Goods updateGoods(Long goodsId, PublishGoodsCommand cmd) {
        List<SkuItem> skuItems = normalizeSkuItems(cmd);
        validateSkuItems(skuItems, cmd.specType());

        Goods goods = this.getByIdWithTenant(goodsId);
        goods.setCategoryIds(toJson(cmd.categoryIds()));
        goods.setBrandId(cmd.brandId());
        goods.setName(cmd.name());
        goods.setSubName(cmd.subName());
        goods.setCode(cmd.code());
        goods.setImages(toJson(cmd.images()));
        goods.setSpecType(resolveSpecType(skuItems, cmd.specType()));
        goods.setContent(cmd.content());
        goods.setDeliveryType(toJson(cmd.deliveryType()));
        goods.setFreightTemplateId(cmd.freightTemplateId());
        goods.setFreightFee(cmd.freightFee());
        goods.setServiceIds(toJson(cmd.serviceIds()));
        goods.setIsVirtual(Boolean.TRUE.equals(cmd.isVirtual()));
        goods.setStockTotal(skuItems.stream().mapToInt(i -> Optional.ofNullable(i.stock()).orElse(0)).sum());
        this.updateById(goods);

        List<GoodsSku> existing = goodsSkuService.listByGoodsId(goodsId);
        Map<String, GoodsSku> existingBySpec = existing.stream()
                .collect(Collectors.toMap(s -> normalizeSpecValueIds(s.getSpecValueIds()), Function.identity(), (a, b) -> a));
        for (SkuItem item : skuItems) {
            String specValueIds = normalizeSpecValueIds(item.specValueIds());
            GoodsSku sku = existingBySpec.remove(specValueIds);
            if (sku == null) {
                sku = new GoodsSku();
                sku.setGoodsId(goodsId);
                sku.setSpecValueIds(specValueIds);
            }
            sku.setSkuCode(item.skuCode());
            sku.setPrice(item.price());
            sku.setLinePrice(item.linePrice());
            sku.setCostPrice(item.costPrice());
            sku.setStock(Optional.ofNullable(item.stock()).orElse(0));
            sku.setWeight(item.weight());
            sku.setVolume(item.volume());
            sku.setImage(item.image());
            if (sku.getId() == null) {
                goodsSkuService.save(sku);
            } else {
                goodsSkuService.updateById(sku);
            }
        }
        // Removed combinations are soft-deleted so historical order snapshots remain intact.
        if (!existingBySpec.isEmpty()) {
            goodsSkuService.removeByIds(existingBySpec.values().stream().map(GoodsSku::getId).toList());
        }
        return goods;
    }

    @Override
    public void updateStatus(Long goodsId, String status) {
        Goods goods = this.getByIdWithTenant(goodsId);
        goods.setStatus(status);
        this.updateById(goods);
    }

    /** 未显式传 SKU 明细时（前端单规格表单只填了一组价格/库存），兜底生成一条空规格的 SkuItem。 */
    private List<SkuItem> normalizeSkuItems(PublishGoodsCommand cmd) {
        if (!CollectionUtils.isEmpty(cmd.skuItems())) {
            return cmd.skuItems();
        }
        throw new BusinessException(ErrorCode.PARAM_INVALID, "商品必须至少包含一个SKU（单规格商品也需要一条默认SKU）");
    }

    private void validateSkuItems(List<SkuItem> skuItems, String specType) {
        for (SkuItem item : skuItems) {
            if (item.price() == null || item.price().signum() < 0) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "SKU价格不能为空或负数");
            }
        }
        if ("multi".equals(specType) && skuItems.size() == 1
                && normalizeSpecValueIds(skuItems.get(0).specValueIds()).isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "多规格商品的SKU必须指定规格值组合");
        }
    }

    /** 单规格固定为 single；多规格只要传入 specType=multi 就尊重调用方声明（哪怕暂时只有一个SKU）。 */
    private String resolveSpecType(List<SkuItem> skuItems, String requestedSpecType) {
        if ("multi".equals(requestedSpecType)) {
            return "multi";
        }
        return "single";
    }

    private String normalizeSpecValueIds(String raw) {
        return raw == null ? "" : raw;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化失败");
        }
    }
}
