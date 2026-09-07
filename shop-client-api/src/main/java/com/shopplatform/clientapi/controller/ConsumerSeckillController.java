package com.shopplatform.clientapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.entity.SeckillTime;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import com.shopplatform.domain.marketing.service.SeckillStockService;
import com.shopplatform.domain.marketing.service.SeckillTimeService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消费者端秒杀专场。对应原型 h5/seckill.html。
 * - GET /api/seckill/sessions：今日场次及状态（upcoming/ongoing/ended）
 * - GET /api/seckill/actives：今日有效活动列表
 * - GET /api/seckill/actives/{id}：活动详情 + 商品列表（含剩余量、限购、原价、秒杀价）
 */
@RestController
@RequestMapping("/api/seckill")
public class ConsumerSeckillController {

    private final SeckillTimeService seckillTimeService;
    private final SeckillActiveService seckillActiveService;
    private final SeckillGoodsService seckillGoodsService;
    private final SeckillStockService seckillStockService;
    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;
    private final ObjectMapper objectMapper;

    public ConsumerSeckillController(SeckillTimeService seckillTimeService,
                                      SeckillActiveService seckillActiveService,
                                      SeckillGoodsService seckillGoodsService,
                                      SeckillStockService seckillStockService,
                                      GoodsService goodsService,
                                      GoodsSkuService goodsSkuService,
                                      ObjectMapper objectMapper) {
        this.seckillTimeService = seckillTimeService;
        this.seckillActiveService = seckillActiveService;
        this.seckillGoodsService = seckillGoodsService;
        this.seckillStockService = seckillStockService;
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        LocalTime now = LocalTime.now();
        List<Map<String, Object>> out = new ArrayList<>();
        for (SeckillTime t : seckillTimeService.listAll()) {
            if (!"on".equals(t.getStatus())) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("startTime", t.getStartTime() == null ? null : t.getStartTime().toString());
            m.put("endTime", t.getEndTime() == null ? null : t.getEndTime().toString());
            String status;
            if (t.getStartTime() != null && now.isBefore(t.getStartTime())) {
                status = "upcoming";
            } else if (t.getEndTime() != null && now.isAfter(t.getEndTime())) {
                status = "ended";
            } else {
                status = "ongoing";
            }
            m.put("status", status);
            out.add(m);
        }
        return Result.ok(out);
    }

    @GetMapping("/actives")
    public Result<List<Map<String, Object>>> actives() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SeckillActive a : seckillActiveService.listOnSale()) {
            out.add(activeView(a, false));
        }
        return Result.ok(out);
    }

    @GetMapping("/actives/{id}")
    public Result<Map<String, Object>> active(@PathVariable Long id) {
        SeckillActive a = seckillActiveService.getByIdWithTenant(id);
        if (a == null || !"on".equals(a.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        Map<String, Object> view = activeView(a, true);
        List<Map<String, Object>> goodsViews = new ArrayList<>();
        for (SeckillGoods g : seckillGoodsService.listByActive(id)) {
            if (!"on".equals(g.getStatus())) {
                continue;
            }
            goodsViews.add(goodsView(g, a));
        }
        view.put("goods", goodsViews);
        return Result.ok(view);
    }

    private Map<String, Object> activeView(SeckillActive a, boolean withTimes) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("startDate", a.getStartDate());
        m.put("endDate", a.getEndDate());
        m.put("remark", a.getRemark());
        boolean isSeckill = StringUtils.hasText(a.getTimeIds()) && !"[]".equals(a.getTimeIds());
        m.put("isSeckill", isSeckill);
        m.put("timeIds", parseIds(a.getTimeIds()));
        if (withTimes && isSeckill) {
            List<SeckillTime> times = seckillTimeService.listByIds(parseIds(a.getTimeIds()));
            List<Map<String, Object>> tv = new ArrayList<>();
            LocalTime now = LocalTime.now();
            for (SeckillTime t : times) {
                Map<String, Object> tm = new HashMap<>();
                tm.put("id", t.getId());
                tm.put("name", t.getName());
                tm.put("startTime", t.getStartTime() == null ? null : t.getStartTime().toString());
                tm.put("endTime", t.getEndTime() == null ? null : t.getEndTime().toString());
                String st;
                if (t.getStartTime() != null && now.isBefore(t.getStartTime())) {
                    st = "upcoming";
                } else if (t.getEndTime() != null && now.isAfter(t.getEndTime())) {
                    st = "ended";
                } else {
                    st = "ongoing";
                }
                tm.put("status", st);
                tv.add(tm);
            }
            m.put("times", tv);
        }
        return m;
    }

    private Map<String, Object> goodsView(SeckillGoods g, SeckillActive a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", g.getId());
        m.put("goodsId", g.getGoodsId());
        m.put("skuId", g.getSkuId());
        m.put("seckillPrice", g.getSeckillPrice());
        m.put("seckillNum", g.getSeckillNum());
        m.put("limitPerUser", g.getLimitPerUser());
        m.put("sold", g.getSold());
        // 剩余量：优先 Redis 实时值，回退 seckill_num - sold
        Integer redis = seckillStockService.getStock(a.getId(), g.getSkuId());
        int sold = g.getSold() == null ? 0 : g.getSold();
        int num = g.getSeckillNum() == null ? 0 : g.getSeckillNum();
        int remaining = redis != null ? redis : Math.max(0, num - sold);
        m.put("remaining", remaining);
        // 商品名/图/原价（不采信前端，统一服务端取）
        try {
            GoodsSku sku = goodsSkuService.getByIdWithTenant(g.getSkuId());
            if (sku != null) {
                m.put("originalPrice", sku.getPrice());
                m.put("image", sku.getImage());
                Goods goods = goodsService.getByIdWithTenant(g.getGoodsId());
                if (goods != null) {
                    m.put("goodsName", goods.getName());
                    if (!StringUtils.hasText(sku.getImage())) {
                        m.put("image", firstImage(goods.getImages()));
                    }
                }
            }
        } catch (Exception e) {
            // 越权/不存在时静默：商品视图字段缺失即可，不阻断专场列表
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

    private List<Long> parseIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Long.class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
