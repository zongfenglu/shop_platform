package com.shopplatform.clientapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.clientapi.dto.GoodsDetail;
import com.shopplatform.clientapi.dto.GoodsListItem;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsCategory;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.entity.GoodsSpec;
import com.shopplatform.domain.goods.entity.GoodsSpecValue;
import com.shopplatform.domain.goods.service.GoodsCategoryService;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.goods.service.GoodsSpecService;
import com.shopplatform.domain.goods.service.GoodsSpecValueService;
import com.shopplatform.domain.goods.support.GoodsCategoryQuery;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消费者端商品浏览：分类树 / 商品列表 / 商品详情。对应原型 h5/home.html、goods-detail.html。
 * <p>
 * 游客可访问，不需要登录态（{@code ClientTenantFilter} 里登录态是可选的）。
 * 三个约束：
 * <ul>
 *   <li>只暴露 {@code status='on'} 的商品。商家"仓库中"的商品即便被猜到 id 也不能通过消费者端查出来
 *       ——所以详情这里不能直接用 {@code getByIdWithTenant} 了事，还要再判一次状态；</li>
 *   <li>返回 DTO 而不是实体：{@code costPrice}（成本价）、{@code salesActual}（真实销量）
 *       属于商家内部数据，不该出现在消费者端响应里；</li>
 *   <li>展示销量 = {@code salesInitial + salesActual}，与商家后台看到的真实销量不是一个口径。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class ConsumerGoodsController {

    private static final int MAX_PAGE_SIZE = 50;

    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;
    private final GoodsCategoryService goodsCategoryService;
    private final GoodsSpecService goodsSpecService;
    private final GoodsSpecValueService goodsSpecValueService;
    private final ObjectMapper objectMapper;

    public ConsumerGoodsController(GoodsService goodsService,
                                    GoodsSkuService goodsSkuService,
                                    GoodsCategoryService goodsCategoryService,
                                    GoodsSpecService goodsSpecService,
                                    GoodsSpecValueService goodsSpecValueService,
                                    ObjectMapper objectMapper) {
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
        this.goodsCategoryService = goodsCategoryService;
        this.goodsSpecService = goodsSpecService;
        this.goodsSpecValueService = goodsSpecValueService;
        this.objectMapper = objectMapper;
    }

    /** 分类树（只返回 is_show=1 的）。装修引擎要到 M4 才接，首页的分类导航先直接用这个。 */
    @GetMapping("/category")
    public Result<List<Map<String, Object>>> categoryTree() {
        List<GoodsCategory> all = goodsCategoryService.list(Wrappers.<GoodsCategory>lambdaQuery()
                .eq(GoodsCategory::getIsShow, true)
                .orderByAsc(GoodsCategory::getSort)
                .orderByAsc(GoodsCategory::getCreateTime));
        Map<Long, List<GoodsCategory>> byParent = all.stream()
                .collect(Collectors.groupingBy(GoodsCategory::getParentId));
        return Result.ok(buildCategoryNodes(byParent, 0L));
    }

    private List<Map<String, Object>> buildCategoryNodes(Map<Long, List<GoodsCategory>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(c -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", c.getId());
                    node.put("name", c.getName());
                    node.put("image", c.getImage());
                    node.put("children", buildCategoryNodes(byParent, c.getId()));
                    return node;
                })
                .toList();
    }

    /**
     * 商品列表。{@code sort} 支持 default(综合)/sales(销量)/price_asc/price_desc。
     * <p>
     * 价格排序按 {@code goods} 表排不了——价格在 SKU 上，一个商品有多个 SKU。这里的做法是
     * 先按其它条件取出当前页，再在应用层按"最低 SKU 价"排序，即**分页内排序**。
     * 严格的全局价格排序需要给 goods 表冗余一个 min_price 列并在发布/改价时维护，
     * 那是 M2 商品域优化的事，现在不做——但要知道当前行为的边界，不要以为它是全局有序的。
     */
    @GetMapping("/goods")
    public Result<Map<String, Object>> list(@RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "default") String sort,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        var wrapper = Wrappers.<Goods>lambdaQuery().eq(Goods::getStatus, "on");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Goods::getName, keyword);
        }
        if (categoryId != null) {
            // 点一级/二级分类时应包含其全部下级；兼容 category_ids 中数字和字符串两种存量格式。
            GoodsCategoryQuery.applyContainsAny(wrapper,
                    goodsCategoryService.listSelfAndDescendantIds(categoryId));
        }
        if ("sales".equals(sort)) {
            wrapper.orderByDesc(Goods::getSalesActual);
        }
        wrapper.orderByDesc(Goods::getSort).orderByDesc(Goods::getCreateTime);

        Page<Goods> page = new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE));
        Page<Goods> result = goodsService.page(page, wrapper);

        List<GoodsListItem> items = toListItems(result.getRecords());
        if ("price_asc".equals(sort)) {
            items = sortByPrice(items, true);
        } else if ("price_desc".equals(sort)) {
            items = sortByPrice(items, false);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("records", items);
        body.put("total", result.getTotal());
        body.put("pageNum", result.getCurrent());
        body.put("pageSize", result.getSize());
        body.put("pages", result.getPages());
        return Result.ok(body);
    }

    private List<GoodsListItem> sortByPrice(List<GoodsListItem> items, boolean asc) {
        Comparator<GoodsListItem> byPrice = Comparator.comparing(
                GoodsListItem::price, Comparator.nullsLast(BigDecimal::compareTo));
        return items.stream().sorted(asc ? byPrice : byPrice.reversed()).toList();
    }

    private List<GoodsListItem> toListItems(List<Goods> goodsList) {
        if (goodsList.isEmpty()) {
            return List.of();
        }
        // 一次性把本页所有商品的 SKU 捞出来按 goodsId 分组，避免每个商品查一次（N+1）。
        List<Long> goodsIds = goodsList.stream().map(Goods::getId).toList();
        Map<Long, List<GoodsSku>> skusByGoods = goodsSkuService
                .list(Wrappers.<GoodsSku>lambdaQuery().in(GoodsSku::getGoodsId, goodsIds))
                .stream().collect(Collectors.groupingBy(GoodsSku::getGoodsId));

        return goodsList.stream().map(g -> {
            List<GoodsSku> skus = skusByGoods.getOrDefault(g.getId(), List.of());
            GoodsSku cheapest = skus.stream()
                    .min(Comparator.comparing(GoodsSku::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                    .orElse(null);
            return new GoodsListItem(
                    g.getId(), g.getName(), g.getSubName(), firstImage(g.getImages()),
                    cheapest == null ? null : cheapest.getPrice(),
                    cheapest == null ? null : cheapest.getLinePrice(),
                    displaySales(g), g.getStockTotal(), g.getSpecType());
        }).toList();
    }

    @GetMapping("/goods/{id}")
    public Result<GoodsDetail> detail(@PathVariable Long id) {
        Goods goods = goodsService.getByIdWithTenant(id);
        if (!"on".equals(goods.getStatus())) {
            // 仓库中/回收站的商品对消费者端等同于不存在——返回"已下架"而不是 404，
            // 是因为用户很可能是从历史订单/分享链接点进来的，这个文案更有意义。
            throw new BusinessException(ErrorCode.GOODS_OFF_SHELF, "商品已下架");
        }
        List<GoodsSku> skus = goodsSkuService.listByGoodsId(id);

        Map<Long, String> specValueNames = loadSpecValueNames(skus);
        List<GoodsDetail.SkuItem> skuItems = skus.stream()
                .map(s -> new GoodsDetail.SkuItem(s.getId(), s.getSpecValueIds(),
                        specText(s.getSpecValueIds(), specValueNames),
                        s.getPrice(), s.getLinePrice(), s.getStock(), s.getImage()))
                .toList();

        BigDecimal minPrice = skus.stream().map(GoodsSku::getPrice)
                .filter(java.util.Objects::nonNull).min(BigDecimal::compareTo).orElse(null);
        BigDecimal maxPrice = skus.stream().map(GoodsSku::getPrice)
                .filter(java.util.Objects::nonNull).max(BigDecimal::compareTo).orElse(null);
        BigDecimal linePrice = skus.stream()
                .min(Comparator.comparing(GoodsSku::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                .map(GoodsSku::getLinePrice).orElse(null);

        return Result.ok(new GoodsDetail(
                goods.getId(), goods.getName(), goods.getSubName(),
                parseJsonArray(goods.getImages()), goods.getVideo(), goods.getContent(),
                goods.getSpecType(), goods.getStatus(), displaySales(goods), goods.getStockTotal(),
                minPrice, maxPrice, linePrice,
                parseJsonArray(goods.getDeliveryType()), goods.getFreightFee(), goods.getFreightTemplateId(),
                goods.getIsVirtual(), buildSpecGroups(skus, specValueNames), skuItems));
    }

    /**
     * 从本商品的 SKU 反推规格树，而不是返回商家整个规格库——商家的规格库里可能有几十个和本商品无关的
     * 规格值（"颜色"下面存着所有商品用过的颜色），全丢给详情页会渲染出一堆点了必然无货的按钮。
     * <p>
     * 规格组的**顺序**取自 {@code spec_value_ids} 串里各段的位置（第1段属于哪个规格，
     * 该规格就排第1），因为 {@code specValueIds} 的匹配是字符串精确比较，前端拼串的顺序
     * 必须和这里返回的组顺序一致，否则"红色/M"永远匹配不到 "M/红色"存的那条 SKU。
     */
    private List<GoodsDetail.SpecGroup> buildSpecGroups(List<GoodsSku> skus, Map<Long, String> specValueNames) {
        // position -> 该位置上出现过的规格值 id（LinkedHashSet 保留首次出现顺序，即商家发布时的枚举顺序）
        Map<Integer, LinkedHashSet<Long>> valueIdsByPosition = new LinkedHashMap<>();
        for (GoodsSku sku : skus) {
            if (!StringUtils.hasText(sku.getSpecValueIds())) {
                continue;
            }
            String[] segments = sku.getSpecValueIds().split("_");
            for (int i = 0; i < segments.length; i++) {
                valueIdsByPosition.computeIfAbsent(i, k -> new LinkedHashSet<>()).add(Long.valueOf(segments[i]));
            }
        }
        if (valueIdsByPosition.isEmpty()) {
            return List.of();
        }

        List<Long> allValueIds = valueIdsByPosition.values().stream().flatMap(LinkedHashSet::stream).toList();
        Map<Long, Long> specIdByValueId = goodsSpecValueService
                .listByIds(allValueIds).stream()
                .collect(Collectors.toMap(GoodsSpecValue::getId, GoodsSpecValue::getSpecId));
        List<Long> specIds = specIdByValueId.values().stream().distinct().toList();
        Map<Long, String> specNames = specIds.isEmpty()
                ? Map.of()
                : goodsSpecService.listByIds(specIds).stream()
                        .collect(Collectors.toMap(GoodsSpec::getId, GoodsSpec::getName));

        List<GoodsDetail.SpecGroup> groups = new ArrayList<>();
        for (Map.Entry<Integer, LinkedHashSet<Long>> entry : valueIdsByPosition.entrySet()) {
            List<Long> valueIds = List.copyOf(entry.getValue());
            Long specId = valueIds.stream().map(specIdByValueId::get)
                    .filter(java.util.Objects::nonNull).findFirst().orElse(null);
            groups.add(new GoodsDetail.SpecGroup(specId,
                    specId == null ? "规格" + (entry.getKey() + 1) : specNames.getOrDefault(specId, "规格"),
                    valueIds.stream()
                            .map(vid -> new GoodsDetail.SpecValueItem(vid, specValueNames.getOrDefault(vid, "")))
                            .toList()));
        }
        return groups;
    }

    private Map<Long, String> loadSpecValueNames(List<GoodsSku> skus) {
        List<Long> ids = skus.stream()
                .map(GoodsSku::getSpecValueIds)
                .filter(StringUtils::hasText)
                .flatMap(s -> java.util.Arrays.stream(s.split("_")))
                .map(Long::valueOf)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return goodsSpecValueService.listByIds(ids).stream()
                .collect(Collectors.toMap(GoodsSpecValue::getId, GoodsSpecValue::getValue));
    }

    private String specText(String specValueIds, Map<Long, String> names) {
        if (!StringUtils.hasText(specValueIds)) {
            return "";
        }
        return java.util.Arrays.stream(specValueIds.split("_"))
                .map(Long::valueOf)
                .map(id -> names.getOrDefault(id, ""))
                .collect(Collectors.joining("/"));
    }

    private Integer displaySales(Goods goods) {
        int initial = goods.getSalesInitial() == null ? 0 : goods.getSalesInitial();
        int actual = goods.getSalesActual() == null ? 0 : goods.getSalesActual();
        return initial + actual;
    }

    private String firstImage(String imagesJson) {
        List<String> images = parseJsonArray(imagesJson);
        return images.isEmpty() ? "" : images.get(0);
    }

    private List<String> parseJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
