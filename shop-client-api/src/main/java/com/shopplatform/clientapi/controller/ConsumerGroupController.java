package com.shopplatform.clientapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.domain.marketing.entity.GroupRecord;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.GroupRecordService;
import com.shopplatform.framework.security.LoginUserContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消费者端拼团。对应原型 h5/group-buy.html。
 * - GET /api/group/actives：上架拼团活动列表
 * - GET /api/group/actives/{id}：活动详情（商品 + 各 SKU 拼团价）
 * - GET /api/group/records/{recordId}：拼团进度（已参团人数/成团人数/状态/截止时间），参团页与分享落地页用
 * 下单走通用 /api/checkout/submit：开团 groupRecordId 传 null，参团传要加入的 recordId。
 */
@RestController
@RequestMapping("/api/group")
public class ConsumerGroupController {

    private final GroupActiveService groupActiveService;
    private final GroupRecordService groupRecordService;
    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;
    private final ObjectMapper objectMapper;

    public ConsumerGroupController(GroupActiveService groupActiveService,
                                     GroupRecordService groupRecordService,
                                     GoodsService goodsService,
                                     GoodsSkuService goodsSkuService,
                                     ObjectMapper objectMapper) {
        this.groupActiveService = groupActiveService;
        this.groupRecordService = groupRecordService;
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/actives")
    public Result<List<Map<String, Object>>> actives() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (GroupActive a : groupActiveService.listOnSale()) {
            out.add(activeView(a, false));
        }
        return Result.ok(out);
    }

    @GetMapping("/actives/{id}")
    public Result<Map<String, Object>> active(@PathVariable Long id) {
        GroupActive a = groupActiveService.getByIdWithTenant(id);
        Map<String, Object> view = activeView(a, true);
        // 各 SKU 拼团价
        Map<Long, BigDecimal> groupPrices = groupActiveService.parseGroupPrice(a);
        List<Map<String, Object>> skus = new ArrayList<>();
        try {
            Goods goods = goodsService.getByIdWithTenant(a.getGoodsId());
            for (GoodsSku sku : goodsSkuService.listByGoodsId(a.getGoodsId())) {
                Map<String, Object> sv = new HashMap<>();
                sv.put("skuId", sku.getId());
                sv.put("price", sku.getPrice());
                sv.put("groupPrice", groupPrices.get(sku.getId()));
                sv.put("stock", sku.getStock());
                sv.put("image", StringUtils.hasText(sku.getImage()) ? sku.getImage() : firstImage(goods.getImages()));
                skus.add(sv);
            }
            view.put("goodsName", goods.getName());
            view.put("goodsImage", firstImage(goods.getImages()));
        } catch (Exception e) {
            // 商品缺失时静默，前端按字段缺失处理
        }
        view.put("skus", skus);
        return Result.ok(view);
    }

    @GetMapping("/records/{recordId}")
    public Result<Map<String, Object>> record(@PathVariable Long recordId) {
        GroupRecord rec = groupRecordService.getByIdWithTenant(recordId);
        GroupActive a = groupActiveService.getByIdWithTenant(rec.getActiveId());
        Map<String, Object> m = new HashMap<>();
        m.put("recordId", rec.getId());
        m.put("activeId", rec.getActiveId());
        m.put("status", rec.getStatus());
        m.put("actualNum", rec.getActualNum());
        m.put("groupNum", a.getGroupNum());
        m.put("expireTime", rec.getExpireTime());
        m.put("successTime", rec.getSuccessTime());
        m.put("leaderUserId", rec.getLeaderUserId());
        // 是否已过期（前端倒计时兜底）
        m.put("expired", rec.getExpireTime() != null && LocalDateTime.now().isAfter(rec.getExpireTime()));
        // 当前用户是否是团长（用于前端展示）
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        m.put("isLeader", loginUser != null && loginUser.userId().equals(rec.getLeaderUserId()));
        return Result.ok(m);
    }

    private Map<String, Object> activeView(GroupActive a, boolean detail) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("goodsId", a.getGoodsId());
        m.put("groupNum", a.getGroupNum());
        m.put("validHours", a.getValidHours());
        m.put("startTime", a.getStartTime());
        m.put("endTime", a.getEndTime());
        m.put("status", a.getStatus());
        // 列表里取最低拼团价做展示
        Map<Long, BigDecimal> prices = groupActiveService.parseGroupPrice(a);
        BigDecimal minPrice = prices.values().stream().reduce(BigDecimal::min).orElse(null);
        m.put("minGroupPrice", minPrice);
        if (detail) {
            try {
                Goods goods = goodsService.getByIdWithTenant(a.getGoodsId());
                m.put("goodsName", goods.getName());
                m.put("goodsImage", firstImage(goods.getImages()));
            } catch (Exception e) {
                // 静默
            }
        }
        return m;
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
