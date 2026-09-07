package com.shopplatform.clientapi.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.entity.DiyPage;
import com.shopplatform.domain.diy.entity.DiyTabbar;
import com.shopplatform.domain.diy.service.DiyPageService;
import com.shopplatform.domain.diy.service.DiyTabbarService;
import com.shopplatform.domain.diy.support.DiyComponentType;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.marketing.entity.BargainActive;
import com.shopplatform.domain.marketing.entity.Coupon;
import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.service.BargainActiveService;
import com.shopplatform.domain.marketing.service.CouponService;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import com.shopplatform.domain.marketing.service.SeckillStockService;
import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.shop.service.PackageFeatureChecker;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消费者端装修渲染：把已发布页面的 {@code pageData} JSON 逐个组件解析成带真实商品/活动数据的视图。
 * <p>
 * 与 {@link CartAppService} 同样的降级原则——任何单个组件引用的商品/活动缺失或套餐权限未开通，
 * 静默跳过该组件而不是让整页渲染失败：渲染是只读展示场景，不该把后端错误码暴露给 C 端用户。
 */
@Service
public class DiyRenderAppService {

    private final DiyPageService diyPageService;
    private final DiyTabbarService diyTabbarService;
    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;
    private final CouponService couponService;
    private final SeckillActiveService seckillActiveService;
    private final SeckillGoodsService seckillGoodsService;
    private final SeckillStockService seckillStockService;
    private final GroupActiveService groupActiveService;
    private final BargainActiveService bargainActiveService;
    private final OfflineStoreService offlineStoreService;
    private final PackageFeatureChecker packageFeatureChecker;
    private final ObjectMapper objectMapper;

    public DiyRenderAppService(DiyPageService diyPageService,
                                DiyTabbarService diyTabbarService,
                                GoodsService goodsService,
                                GoodsSkuService goodsSkuService,
                                CouponService couponService,
                                SeckillActiveService seckillActiveService,
                                SeckillGoodsService seckillGoodsService,
                                SeckillStockService seckillStockService,
                                GroupActiveService groupActiveService,
                                BargainActiveService bargainActiveService,
                                OfflineStoreService offlineStoreService,
                                PackageFeatureChecker packageFeatureChecker,
                                ObjectMapper objectMapper) {
        this.diyPageService = diyPageService;
        this.diyTabbarService = diyTabbarService;
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
        this.couponService = couponService;
        this.seckillActiveService = seckillActiveService;
        this.seckillGoodsService = seckillGoodsService;
        this.seckillStockService = seckillStockService;
        this.groupActiveService = groupActiveService;
        this.bargainActiveService = bargainActiveService;
        this.offlineStoreService = offlineStoreService;
        this.packageFeatureChecker = packageFeatureChecker;
        this.objectMapper = objectMapper;
    }

    /** 无首页（新店铺尚未装修）是正常态，返回 {@code {exists:false}} 而不是 404。 */
    public Map<String, Object> renderHome(Long shopId) {
        DiyPage home = diyPageService.getDefaultHome(shopId);
        if (home == null || home.getPageData() == null) {
            return Map.of("exists", false);
        }
        return buildPageView(home, shopId);
    }

    public Map<String, Object> renderPage(Long shopId, Long pageId) {
        DiyPage page;
        try {
            page = diyPageService.getByIdWithTenant(pageId);
        } catch (TenantAccessDeniedException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在");
        }
        if (page.getPageData() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在");
        }
        return buildPageView(page, shopId);
    }

    public Map<String, Object> renderTabbar(Long shopId) {
        Map<String, Object> out = new LinkedHashMap<>();
        DiyTabbar tabbar = diyTabbarService.peek();
        if (tabbar == null) {
            out.put("items", List.of());
            out.put("style", null);
            return out;
        }
        try {
            out.put("items", objectMapper.readValue(tabbar.getItems(), new TypeReference<List<Map<String, Object>>>() {}));
        } catch (Exception e) {
            out.put("items", List.of());
        }
        if (StringUtils.hasText(tabbar.getStyle())) {
            try {
                out.put("style", objectMapper.readValue(tabbar.getStyle(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                out.put("style", null);
            }
        } else {
            out.put("style", null);
        }
        return out;
    }

    private Map<String, Object> buildPageView(DiyPage page, Long shopId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exists", true);
        out.put("pageId", page.getId());
        out.put("name", page.getName());
        out.put("pageType", page.getPageType());
        out.put("version", page.getVersion());
        out.put("page", resolvePageMeta(page.getPageData()));
        out.put("items", resolveItems(page.getPageData(), shopId));
        return out;
    }

    /** 页面级样式（背景色/背景图），缺省返回空对象，前端走默认底色。 */
    private Map<String, Object> resolvePageMeta(String pageDataJson) {
        try {
            JsonNode root = objectMapper.readTree(pageDataJson);
            JsonNode page = root == null ? null : root.get("page");
            if (page == null || !page.isObject()) {
                return Map.of();
            }
            Map<String, Object> meta = new LinkedHashMap<>();
            putText(page, meta, "bgType");
            putText(page, meta, "bgColor");
            putText(page, meta, "bgImage");
            return meta;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static void putText(JsonNode node, Map<String, Object> out, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual() && !value.asText().isBlank()) {
            out.put(field, value.asText());
        }
    }

    private List<Map<String, Object>> resolveItems(String pageDataJson, Long shopId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(pageDataJson);
        } catch (Exception e) {
            return List.of();
        }
        JsonNode items = root == null ? null : root.get("items");
        if (items == null || !items.isArray()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode item : items) {
            try {
                Map<String, Object> resolved = resolveItem(item, shopId);
                if (resolved != null) {
                    out.add(resolved);
                }
            } catch (Exception e) {
                // 单个组件解析失败（引用的商品/活动已被删除等）不影响整页渲染，静默跳过
            }
        }
        return out;
    }

    private Map<String, Object> resolveItem(JsonNode item, Long shopId) {
        String type = item.path("type").asText(null);
        Optional<DiyComponentType> componentType = DiyComponentType.fromType(type);
        if (componentType.isEmpty()) {
            return null;
        }
        String requiredMenu = componentType.get().getRequiredMenu();
        if (requiredMenu != null && !packageFeatureChecker.hasMenu(shopId, requiredMenu)) {
            return null;
        }
        return switch (type) {
            case "goods" -> resolveGoods(item);
            case "coupon" -> resolveCoupon(item);
            case "seckill" -> resolveSeckill(item);
            case "group" -> resolveGroup(item);
            case "bargain" -> resolveBargain(item);
            case "store" -> resolveStore(item);
            default -> toMap(item);
        };
    }

    private Map<String, Object> resolveGoods(JsonNode item) {
        Map<String, Object> base = toMap(item);
        List<Long> ids = parseLongArray(item.get("goodsIds"));
        if (ids.isEmpty()) {
            base.put("goodsList", List.of());
            return base;
        }
        Map<Long, Goods> goodsById = goodsService.listByIds(ids).stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));
        Map<Long, List<GoodsSku>> skusByGoods = goodsSkuService
                .list(Wrappers.<GoodsSku>lambdaQuery().in(GoodsSku::getGoodsId, ids))
                .stream().collect(Collectors.groupingBy(GoodsSku::getGoodsId));

        List<Map<String, Object>> views = new ArrayList<>();
        for (Long id : ids) {
            Goods g = goodsById.get(id);
            if (g == null || !"on".equals(g.getStatus())) {
                continue;
            }
            List<GoodsSku> skus = skusByGoods.getOrDefault(id, List.of());
            GoodsSku cheapest = skus.stream()
                    .min(Comparator.comparing(GoodsSku::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                    .orElse(null);
            Map<String, Object> gv = new LinkedHashMap<>();
            gv.put("id", g.getId());
            gv.put("name", g.getName());
            gv.put("image", firstImage(g.getImages()));
            gv.put("price", cheapest == null ? null : cheapest.getPrice());
            gv.put("linePrice", cheapest == null ? null : cheapest.getLinePrice());
            views.add(gv);
        }
        base.put("goodsList", views);
        return base;
    }

    private Map<String, Object> resolveCoupon(JsonNode item) {
        Map<String, Object> base = toMap(item);
        Set<Long> ids = new HashSet<>(parseLongArray(item.get("couponIds")));
        List<Map<String, Object>> views = couponService.listReceivable().stream()
                .filter(c -> ids.contains(c.getId()))
                .map(this::couponView)
                .toList();
        base.put("couponList", views);
        return base;
    }

    private Map<String, Object> couponView(Coupon c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("type", c.getType());
        m.put("reducePrice", c.getReducePrice());
        m.put("discountRatio", c.getDiscountRatio());
        m.put("minPrice", c.getMinPrice());
        return m;
    }

    /** 秒杀活动已下架/删除时返回 null，整个组件被 {@link #resolveItems} 静默跳过。 */
    private Map<String, Object> resolveSeckill(JsonNode item) {
        Long activeId = readLong(item.get("activeId"));
        Map<String, Object> base = toMap(item);
        if (activeId == null) {
            return base;
        }
        SeckillActive active = seckillActiveService.listOnSale().stream()
                .filter(a -> a.getId().equals(activeId)).findFirst().orElse(null);
        if (active == null) {
            return null;
        }
        Map<String, Object> av = new LinkedHashMap<>();
        av.put("id", active.getId());
        av.put("name", active.getName());
        av.put("startDate", active.getStartDate());
        av.put("endDate", active.getEndDate());

        List<Map<String, Object>> goodsViews = new ArrayList<>();
        for (SeckillGoods g : seckillGoodsService.listByActive(activeId)) {
            if (!"on".equals(g.getStatus())) {
                continue;
            }
            try {
                GoodsSku sku = goodsSkuService.getByIdWithTenant(g.getSkuId());
                Goods goods = goodsService.getByIdWithTenant(g.getGoodsId());
                Map<String, Object> gv = new LinkedHashMap<>();
                gv.put("goodsId", g.getGoodsId());
                gv.put("skuId", g.getSkuId());
                gv.put("goodsName", goods.getName());
                gv.put("image", StringUtils.hasText(sku.getImage()) ? sku.getImage() : firstImage(goods.getImages()));
                gv.put("seckillPrice", g.getSeckillPrice());
                gv.put("originalPrice", sku.getPrice());
                Integer redis = seckillStockService.getStock(activeId, g.getSkuId());
                int sold = g.getSold() == null ? 0 : g.getSold();
                int num = g.getSeckillNum() == null ? 0 : g.getSeckillNum();
                gv.put("remaining", redis != null ? redis : Math.max(0, num - sold));
                goodsViews.add(gv);
            } catch (Exception e) {
                // 该秒杀商品对应的商品/SKU缺失，静默跳过这一条，不影响活动其余商品
            }
        }
        int limit = item.path("limit").asInt(0);
        if (limit > 0 && goodsViews.size() > limit) {
            goodsViews = new ArrayList<>(goodsViews.subList(0, limit));
        }
        av.put("goods", goodsViews);
        base.put("active", av);
        return base;
    }

    private Map<String, Object> resolveGroup(JsonNode item) {
        Map<String, Object> base = toMap(item);
        List<GroupActive> onSale = groupActiveService.listOnSale();
        List<GroupActive> picked = pickActives(onSale, item, GroupActive::getId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (GroupActive active : picked) {
            Map<String, Object> av = new LinkedHashMap<>();
            av.put("id", active.getId());
            av.put("groupNum", active.getGroupNum());
            Map<Long, BigDecimal> prices = groupActiveService.parseGroupPrice(active);
            av.put("minGroupPrice", prices.values().stream().reduce(BigDecimal::min).orElse(null));
            fillGoodsCard(av, active.getGoodsId());
            list.add(av);
        }
        base.put("list", list);
        if (!list.isEmpty()) {
            base.put("active", list.get(0));
        }
        return base;
    }

    private Map<String, Object> resolveBargain(JsonNode item) {
        Map<String, Object> base = toMap(item);
        List<BargainActive> onSale = bargainActiveService.listOnSale();
        List<BargainActive> picked = pickActives(onSale, item, BargainActive::getId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (BargainActive active : picked) {
            Map<String, Object> av = new LinkedHashMap<>();
            av.put("id", active.getId());
            av.put("floorPrice", active.getFloorPrice());
            av.put("helpLimit", active.getHelpLimit());
            fillGoodsCard(av, active.getGoodsId());
            list.add(av);
        }
        base.put("list", list);
        if (!list.isEmpty()) {
            base.put("active", list.get(0));
        }
        return base;
    }

    private Map<String, Object> resolveStore(JsonNode item) {
        Map<String, Object> base = toMap(item);
        List<OfflineStore> enabled = offlineStoreService.listEnabled();
        Set<Long> ids = new HashSet<>(parseLongArray(item.get("storeIds")));
        boolean manual = "manual".equals(item.path("source").asText("auto"));
        List<Map<String, Object>> views = new ArrayList<>();
        for (OfflineStore store : enabled) {
            if (manual && !ids.isEmpty() && !ids.contains(store.getId())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", store.getId());
            row.put("name", store.getName());
            row.put("logo", store.getLogo());
            row.put("phone", store.getPhone());
            String region = store.getRegion() == null ? "" : store.getRegion();
            String detail = store.getDetail() == null ? "" : store.getDetail();
            row.put("address", (region + detail).trim());
            views.add(row);
        }
        base.put("storeList", views);
        return base;
    }

    private void fillGoodsCard(Map<String, Object> av, Long goodsId) {
        if (goodsId == null) {
            return;
        }
        try {
            Goods goods = goodsService.getByIdWithTenant(goodsId);
            av.put("goodsId", goods.getId());
            av.put("goodsName", goods.getName());
            av.put("sellingPoint", goods.getSubName());
            av.put("goodsImage", firstImage(goods.getImages()));
            List<GoodsSku> skus = goodsSkuService.list(Wrappers.<GoodsSku>lambdaQuery().eq(GoodsSku::getGoodsId, goodsId));
            GoodsSku cheapest = skus.stream()
                    .min(Comparator.comparing(GoodsSku::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                    .orElse(null);
            if (cheapest != null) {
                av.put("originalPrice", cheapest.getLinePrice() != null ? cheapest.getLinePrice() : cheapest.getPrice());
            }
        } catch (Exception e) {
            // 关联商品缺失时静默，活动本身仍展示
        }
    }

    private <T> List<T> pickActives(List<T> onSale, JsonNode item, java.util.function.Function<T, Long> idFn) {
        int limit = Math.max(1, item.path("limit").asInt(6));
        List<Long> ids = parseLongArray(item.get("activeIds"));
        Long single = readLong(item.get("activeId"));
        if (ids.isEmpty() && single != null) {
            ids = List.of(single);
        }
        if (ids.isEmpty() || "auto".equals(item.path("source").asText("auto"))) {
            return onSale.stream().limit(limit).toList();
        }
        Set<Long> want = new HashSet<>(ids);
        return onSale.stream().filter(a -> want.contains(idFn.apply(a))).limit(limit).toList();
    }

    private Map<String, Object> toMap(JsonNode item) {
        return objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {
        });
    }

    private Long readLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        if (node.isTextual() && !node.asText().isBlank()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private List<Long> parseLongArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        node.forEach(n -> {
            Long id = readLong(n);
            if (id != null) {
                ids.add(id);
            }
        });
        return ids;
    }

    private String firstImage(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return "";
        }
        try {
            List<String> images = objectMapper.readValue(imagesJson,
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));
            return images.isEmpty() ? "" : images.get(0);
        } catch (Exception e) {
            return "";
        }
    }
}
