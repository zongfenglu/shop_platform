package com.shopplatform.clientapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.clientapi.dto.CartItemView;
import com.shopplatform.domain.cart.entity.Cart;
import com.shopplatform.domain.cart.service.CartService;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.entity.GoodsSpecValue;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.goods.service.GoodsSpecValueService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车展示视图的组装：把 {@code cart} 里存的 (skuId, quantity) 还原成带商品名/规格/实时价/库存的行。
 * <p>
 * 与 {@link CheckoutAppService} 同样的原则——价格一律以服务端 SKU 记录为准。
 * 失效行（商品下架、SKU 被删）不从库里清掉，而是标记 {@code invalid} 返回给前端置灰展示，
 * 让用户自己知道"我加过的这个东西没了"，而不是无声消失。
 */
@Service
public class CartAppService {

    private final CartService cartService;
    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;
    private final GoodsSpecValueService goodsSpecValueService;
    private final ObjectMapper objectMapper;

    public CartAppService(CartService cartService,
                           GoodsService goodsService,
                           GoodsSkuService goodsSkuService,
                           GoodsSpecValueService goodsSpecValueService,
                           ObjectMapper objectMapper) {
        this.cartService = cartService;
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
        this.goodsSpecValueService = goodsSpecValueService;
        this.objectMapper = objectMapper;
    }

    public List<CartItemView> listView(Long userId) {
        List<Cart> carts = cartService.listByUser(userId);
        if (carts.isEmpty()) {
            return List.of();
        }

        // 批量取 SKU / 商品，避免每行各查一次。逻辑删除的 SKU 在这里直接查不出来 → 该行判为失效。
        Map<Long, GoodsSku> skuById = goodsSkuService
                .listByIds(carts.stream().map(Cart::getSkuId).distinct().toList())
                .stream().collect(Collectors.toMap(GoodsSku::getId, s -> s));
        Map<Long, Goods> goodsById = goodsService
                .listByIds(carts.stream().map(Cart::getGoodsId).distinct().toList())
                .stream().collect(Collectors.toMap(Goods::getId, g -> g));
        Map<Long, String> specValueNames = loadSpecValueNames(skuById.values());

        return carts.stream().map(cart -> {
            GoodsSku sku = skuById.get(cart.getSkuId());
            Goods goods = goodsById.get(cart.getGoodsId());
            if (sku == null || goods == null) {
                return new CartItemView(cart.getId(), cart.getGoodsId(), cart.getSkuId(),
                        "商品已删除", "", "", null, null, cart.getQuantity(), 0, true, "商品已删除");
            }
            boolean offShelf = !"on".equals(goods.getStatus());
            boolean outOfStock = sku.getStock() == null || sku.getStock() <= 0;
            String reason = offShelf ? "商品已下架" : (outOfStock ? "库存不足" : null);
            String image = StringUtils.hasText(sku.getImage()) ? sku.getImage() : firstImage(goods.getImages());

            return new CartItemView(cart.getId(), goods.getId(), sku.getId(), goods.getName(),
                    specText(sku.getSpecValueIds(), specValueNames), image,
                    sku.getPrice(), sku.getLinePrice(), cart.getQuantity(), sku.getStock(),
                    reason != null, reason);
        }).toList();
    }

    private Map<Long, String> loadSpecValueNames(java.util.Collection<GoodsSku> skus) {
        List<Long> ids = skus.stream()
                .map(GoodsSku::getSpecValueIds)
                .filter(StringUtils::hasText)
                .flatMap(s -> Arrays.stream(s.split("_")))
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
        return Arrays.stream(specValueIds.split("_"))
                .map(Long::valueOf)
                .map(id -> names.getOrDefault(id, ""))
                .collect(Collectors.joining("/"));
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
