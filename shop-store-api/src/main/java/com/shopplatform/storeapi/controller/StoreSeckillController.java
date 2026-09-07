package com.shopplatform.storeapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
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

import java.util.List;

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
    private final ObjectMapper objectMapper;

    public StoreSeckillController(SeckillTimeService seckillTimeService,
                                    SeckillActiveService seckillActiveService,
                                    SeckillGoodsService seckillGoodsService,
                                    SeckillStockService seckillStockService,
                                    ObjectMapper objectMapper) {
        this.seckillTimeService = seckillTimeService;
        this.seckillActiveService = seckillActiveService;
        this.seckillGoodsService = seckillGoodsService;
        this.seckillStockService = seckillStockService;
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
        seckillTimeService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveSeckillTimeRequest req, SeckillTime t) {
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
        seckillActiveService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveSeckillActiveRequest req, SeckillActive a) {
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

    // ---------- 活动商品 ----------

    @GetMapping("/actives/{activeId}/goods")
    public Result<List<SeckillGoods>> listGoods(@PathVariable Long activeId) {
        return Result.ok(seckillGoodsService.listByActive(activeId));
    }

    @PostMapping("/goods")
    public Result<SeckillGoods> createGoods(@Valid @RequestBody SaveSeckillGoodsRequest req) {
        SeckillGoods g = new SeckillGoods();
        apply(req, g);
        seckillGoodsService.save(g);
        // 初始化 Redis 秒杀限量池（秒杀才需要；限时折扣 seckillNum=0 也写一个 0，预扣时不会用到）
        initRedisStock(g);
        return Result.ok(g);
    }

    @PutMapping("/goods/{id}")
    public Result<SeckillGoods> updateGoods(@PathVariable Long id, @Valid @RequestBody SaveSeckillGoodsRequest req) {
        SeckillGoods g = seckillGoodsService.getByIdWithTenant(id);
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

    private void apply(SaveSeckillGoodsRequest req, SeckillGoods g) {
        g.setActiveId(req.activeId());
        g.setGoodsId(req.goodsId());
        g.setSkuId(req.skuId());
        g.setSeckillPrice(req.seckillPrice());
        g.setSeckillNum(req.seckillNum() == null ? 0 : req.seckillNum());
        g.setLimitPerUser(req.limitPerUser() == null ? 0 : req.limitPerUser());
        g.setSold(0);
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
