package com.shopplatform.storeapi.controller;

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
import com.shopplatform.storeapi.dto.SaveSeckillActiveRequest;
import com.shopplatform.storeapi.dto.SaveSeckillGoodsRequest;
import com.shopplatform.storeapi.dto.SaveSeckillTimeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商户后台秒杀/限时折扣配置：场次、活动、活动商品 CRUD。
 * 对应原型 docs/prototype/store/marketing-seckill.html。
 * 限时折扣与秒杀共用 seckill_active：time_ids 空 = 限时折扣（全天仅换价、无限购预扣）。
 */
@RestController
@RequestMapping("/store/seckill")
public class StoreSeckillController {

    private final SeckillTimeService seckillTimeService;
    private final SeckillActiveService seckillActiveService;
    private final SeckillGoodsService seckillGoodsService;
    private final SeckillStockService seckillStockService;
    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;
    private final ObjectMapper objectMapper;

    public StoreSeckillController(SeckillTimeService seckillTimeService,
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

    // ---------- 场次 ----------

    @GetMapping("/times")
    public Result<List<SeckillTime>> listTimes() {
        return Result.ok(seckillTimeService.listAll());
    }

    @PostMapping("/times")
    public Result<SeckillTime> createTime(@Valid @RequestBody SaveSeckillTimeRequest req) {
        SeckillTime t = new SeckillTime();
        apply(req, t);
        seckillTimeService.save(t);
        return Result.ok(t);
    }

    @PutMapping("/times/{id}")
    public Result<SeckillTime> updateTime(@PathVariable Long id, @Valid @RequestBody SaveSeckillTimeRequest req) {
        SeckillTime t = seckillTimeService.getByIdWithTenant(id);
        apply(req, t);
        seckillTimeService.updateById(t);
        return Result.ok(t);
    }

    @DeleteMapping("/times/{id}")
    public Result<Void> deleteTime(@PathVariable Long id) {
        seckillTimeService.getByIdWithTenant(id);
        // 场次与活动联动：被活动引用的场次不允许直接删除，否则活动会指向不存在的场次
        for (SeckillActive active : seckillActiveService.listAll()) {
            if (parseTimeIds(active.getTimeIds()).contains(id)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID,
                        "该场次已被活动「" + active.getName() + "」使用，请先编辑活动移除该场次");
            }
        }
        seckillTimeService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveSeckillTimeRequest req, SeckillTime t) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "场次开始时间必须早于结束时间");
        }
        t.setName(req.name());
        t.setStartTime(req.startTime());
        t.setEndTime(req.endTime());
        t.setSort(req.sort() == null ? 0 : req.sort());
        t.setStatus(req.status() == null ? "on" : req.status());
    }

    // ---------- 活动 ----------

    @GetMapping("/actives")
    public Result<List<SeckillActive>> listActives() {
        return Result.ok(seckillActiveService.listAll());
    }

    @PostMapping("/actives")
    public Result<SeckillActive> createActive(@Valid @RequestBody SaveSeckillActiveRequest req) {
        SeckillActive a = new SeckillActive();
        apply(req, a);
        seckillActiveService.save(a);
        return Result.ok(a);
    }

    @PutMapping("/actives/{id}")
    public Result<SeckillActive> updateActive(@PathVariable Long id, @Valid @RequestBody SaveSeckillActiveRequest req) {
        SeckillActive a = seckillActiveService.getByIdWithTenant(id);
        apply(req, a);
        seckillActiveService.updateById(a);
        return Result.ok(a);
    }

    @DeleteMapping("/actives/{id}")
    public Result<Void> deleteActive(@PathVariable Long id) {
        seckillActiveService.getByIdWithTenant(id);
        // 活动删除时同步清理其活动商品与 Redis 限量池，避免留下不可见的孤儿行
        for (SeckillGoods g : seckillGoodsService.listByActive(id)) {
            seckillStockService.evictStock(g.getActiveId(), g.getSkuId());
        }
        seckillGoodsService.removeByActive(id);
        seckillActiveService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveSeckillActiveRequest req, SeckillActive a) {
        if (req.startDate().isAfter(req.endDate())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "活动开始日期不能晚于结束日期");
        }
        if (req.timeIds() != null && !req.timeIds().isEmpty()) {
            Set<Long> existing = new HashSet<>();
            for (SeckillTime t : seckillTimeService.listAll()) {
                existing.add(t.getId());
            }
            for (Long timeId : req.timeIds()) {
                if (!existing.contains(timeId)) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID, "所选场次不存在或已删除，请刷新后重试");
                }
            }
        }
        a.setName(req.name());
        a.setTimeIds(toJson(req.timeIds()));
        a.setStartDate(req.startDate());
        a.setEndDate(req.endDate());
        a.setStatus(req.status() == null ? "on" : req.status());
        a.setRemark(req.remark());
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null; // 限时折扣
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化场次失败");
        }
    }

    private List<Long> parseTimeIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    // ---------- 活动商品 ----------

    @GetMapping("/actives/{activeId}/goods")
    public Result<List<SeckillGoods>> listGoods(@PathVariable Long activeId) {
        return Result.ok(seckillGoodsService.listByActive(activeId));
    }

    @PostMapping("/goods")
    public Result<SeckillGoods> createGoods(@Valid @RequestBody SaveSeckillGoodsRequest req) {
        validateGoods(req, null);
        SeckillGoods g = new SeckillGoods();
        apply(req, g);
        g.setSold(0);
        seckillGoodsService.save(g);
        // 初始化 Redis 秒杀限量池（秒杀才需要；限时折扣 seckillNum=0 也写一个 0，预扣时不会用到）
        initRedisStock(g);
        return Result.ok(g);
    }

    @PutMapping("/goods/{id}")
    public Result<SeckillGoods> updateGoods(@PathVariable Long id, @Valid @RequestBody SaveSeckillGoodsRequest req) {
        SeckillGoods g = seckillGoodsService.getByIdWithTenant(id);
        validateGoods(req, id);
        apply(req, g);
        seckillGoodsService.updateById(g);
        initRedisStock(g);
        return Result.ok(g);
    }

    @DeleteMapping("/goods/{id}")
    public Result<Void> deleteGoods(@PathVariable Long id) {
        SeckillGoods g = seckillGoodsService.getByIdWithTenant(id);
        seckillGoodsService.removeById(id);
        seckillStockService.evictStock(g.getActiveId(), g.getSkuId());
        return Result.ok();
    }

    /**
     * 活动商品入库前的业务校验。excludeId 编辑时排除自身。
     * 这里逐条给出可读的失败原因，而不是靠 uk_active_sku 兜底——唯一键冲突会被全局兜底
     * 处理成"系统繁忙"，运营无从知道是重复添加了商品。
     */
    private void validateGoods(SaveSeckillGoodsRequest req, Long excludeId) {
        seckillActiveService.getByIdWithTenant(req.activeId());
        Goods goods = goodsService.getByIdWithTenant(req.goodsId());
        if (!"on".equals(goods.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "商品「" + goods.getName() + "」当前不在售（仓库中/已删除），请先上架再参与活动");
        }
        GoodsSku sku = goodsSkuService.getByIdWithTenant(req.skuId());
        if (!req.goodsId().equals(sku.getGoodsId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "所选规格不属于该商品，请重新选择");
        }
        if (req.seckillPrice().signum() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "秒杀价必须大于 0");
        }
        if (seckillGoodsService.existsByActiveAndSku(req.activeId(), req.skuId(), excludeId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该商品规格已在当前活动中，请勿重复添加");
        }
    }

    private void apply(SaveSeckillGoodsRequest req, SeckillGoods g) {
        g.setActiveId(req.activeId());
        g.setGoodsId(req.goodsId());
        g.setSkuId(req.skuId());
        g.setSeckillPrice(req.seckillPrice());
        g.setSeckillNum(req.seckillNum() == null ? 0 : req.seckillNum());
        g.setLimitPerUser(req.limitPerUser() == null ? 0 : req.limitPerUser());
        g.setStatus(req.status() == null ? "on" : req.status());
        g.setSort(req.sort() == null ? 0 : req.sort());
    }

    private void initRedisStock(SeckillGoods g) {
        if (TenantContext.get() == null) {
            return;
        }
        int num = g.getSeckillNum() == null ? 0 : g.getSeckillNum();
        seckillStockService.initStock(g.getActiveId(), g.getSkuId(), num);
    }
}
