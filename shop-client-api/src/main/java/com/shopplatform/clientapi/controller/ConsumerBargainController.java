package com.shopplatform.clientapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.marketing.entity.BargainActive;
import com.shopplatform.domain.marketing.entity.BargainRecord;
import com.shopplatform.domain.marketing.service.BargainActiveService;
import com.shopplatform.domain.marketing.service.BargainRecordService;
import com.shopplatform.framework.security.LoginUserContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 消费者端砍价。对应原型 h5/bargain.html。
 * - GET /api/bargain/actives：上架砍价活动列表
 * - GET /api/bargain/actives/{id}：活动详情（商品 + 底价）
 * - POST /api/bargain/start：发起砍价（body: activeId, skuId），current_price=SKU 原价
 * - POST /api/bargain/help/{recordId}：好友助力砍一刀，current_price 递减至 floor_price 或 help_limit
 * - GET /api/bargain/records/{recordId}：砍价进度（当前价/底价/已助力次数/截止时间）
 * 下单走通用 /api/checkout/submit：activityType=bargain，activityId=bargain_record.id。
 */
@RestController
@RequestMapping("/api/bargain")
public class ConsumerBargainController {

    private final BargainActiveService bargainActiveService;
    private final BargainRecordService bargainRecordService;
    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;

    public ConsumerBargainController(BargainActiveService bargainActiveService,
                                      BargainRecordService bargainRecordService,
                                      GoodsService goodsService,
                                      GoodsSkuService goodsSkuService) {
        this.bargainActiveService = bargainActiveService;
        this.bargainRecordService = bargainRecordService;
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
    }

    @GetMapping("/actives")
    public Result<List<Map<String, Object>>> actives() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (BargainActive a : bargainActiveService.listOnSale()) {
            out.add(activeView(a, false));
        }
        return Result.ok(out);
    }

    @GetMapping("/actives/{id}")
    public Result<Map<String, Object>> active(@PathVariable Long id) {
        BargainActive a = bargainActiveService.getByIdWithTenant(id);
        Map<String, Object> view = activeView(a, true);
        // 各 SKU 原价（砍价从原价开始砍到底价）
        List<Map<String, Object>> skus = new ArrayList<>();
        try {
            Goods goods = goodsService.getByIdWithTenant(a.getGoodsId());
            for (GoodsSku sku : goodsSkuService.listByGoodsId(a.getGoodsId())) {
                Map<String, Object> sv = new HashMap<>();
                sv.put("skuId", sku.getId());
                sv.put("price", sku.getPrice());
                sv.put("stock", sku.getStock());
                sv.put("image", StringUtils.hasText(sku.getImage()) ? sku.getImage() : firstImage(goods.getImages()));
                skus.add(sv);
            }
            view.put("goodsName", goods.getName());
            view.put("goodsImage", firstImage(goods.getImages()));
        } catch (Exception e) {
            // 静默
        }
        view.put("skus", skus);
        return Result.ok(view);
    }

    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestBody StartRequest req) {
        Long userId = requireLoginUserId();
        BargainActive a = bargainActiveService.getByIdWithTenant(req.activeId());
        if (!"on".equals(a.getStatus())) {
            throw new BusinessException(ErrorCode.BARGAIN_NOT_FOUND, "砍价活动不存在或已结束");
        }
        if (LocalDateTime.now().isBefore(a.getStartTime()) || LocalDateTime.now().isAfter(a.getEndTime())) {
            throw new BusinessException(ErrorCode.BARGAIN_EXPIRED, "砍价活动未在有效期内");
        }
        GoodsSku sku = goodsSkuService.getByIdWithTenant(req.skuId());
        if (sku == null || !sku.getGoodsId().equals(a.getGoodsId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "SKU 不属于该砍价商品");
        }
        BargainRecord rec = bargainRecordService.startBargain(a.getId(), userId, sku.getPrice(),
                a.getValidHours() == null ? 24 : a.getValidHours());
        return Result.ok(recordView(rec, a));
    }

    @PostMapping("/help/{recordId}")
    public Result<Map<String, Object>> help(@PathVariable Long recordId) {
        // 助力者需登录（可不同于发起人）
        requireLoginUserId();
        BargainRecord rec = bargainRecordService.getByIdWithTenant(recordId);
        if (!"ongoing".equals(rec.getStatus())) {
            throw new BusinessException(ErrorCode.BARGAIN_FLOOR_REACHED, "砍价已结束");
        }
        if (rec.getExpireTime() != null && LocalDateTime.now().isAfter(rec.getExpireTime())) {
            throw new BusinessException(ErrorCode.BARGAIN_EXPIRED, "砍价已过期");
        }
        BargainActive a = bargainActiveService.getByIdWithTenant(rec.getActiveId());
        // 助力次数上限校验
        int helpLimit = a.getHelpLimit() == null ? 0 : a.getHelpLimit();
        if (helpLimit > 0 && rec.getHelpCount() != null && rec.getHelpCount() >= helpLimit) {
            throw new BusinessException(ErrorCode.BARGAIN_HELP_LIMIT, "砍价助力次数已达上限");
        }
        // 砍价金额：剩余可砍空间的 5%~15% 随机，至少 0.01
        BigDecimal remaining = rec.getCurrentPrice().subtract(a.getFloorPrice());
        BigDecimal cut;
        if (remaining.signum() <= 0) {
            cut = BigDecimal.ZERO;
        } else {
            double ratio = 0.05 + ThreadLocalRandom.current().nextDouble(0.10);
            cut = remaining.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.DOWN);
            if (cut.signum() <= 0) {
                cut = new BigDecimal("0.01");
            }
        }
        BargainRecord updated = bargainRecordService.helpCut(recordId, cut, a.getFloorPrice());
        if (updated == null) {
            throw new BusinessException(ErrorCode.BARGAIN_FLOOR_REACHED, "砍价已到底价或已结束");
        }
        return Result.ok(recordView(updated, a));
    }

    @GetMapping("/records/{recordId}")
    public Result<Map<String, Object>> record(@PathVariable Long recordId) {
        BargainRecord rec = bargainRecordService.getByIdWithTenant(recordId);
        BargainActive a = bargainActiveService.getByIdWithTenant(rec.getActiveId());
        return Result.ok(recordView(rec, a));
    }

    private Map<String, Object> recordView(BargainRecord rec, BargainActive a) {
        Map<String, Object> m = new HashMap<>();
        m.put("recordId", rec.getId());
        m.put("activeId", rec.getActiveId());
        m.put("currentPrice", rec.getCurrentPrice());
        m.put("floorPrice", a.getFloorPrice());
        m.put("helpCount", rec.getHelpCount());
        m.put("helpLimit", a.getHelpLimit());
        m.put("status", rec.getStatus());
        m.put("expireTime", rec.getExpireTime());
        m.put("expired", rec.getExpireTime() != null && LocalDateTime.now().isAfter(rec.getExpireTime()));
        // 已砍掉金额
        BigDecimal origin = rec.getCurrentPrice() != null && a.getFloorPrice() != null
                ? rec.getCurrentPrice().subtract(a.getFloorPrice()) : BigDecimal.ZERO;
        m.put("cutAmount", rec.getCurrentPrice() == null ? BigDecimal.ZERO : rec.getCurrentPrice().subtract(a.getFloorPrice()).max(BigDecimal.ZERO));
        // 当前用户是否是发起人
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        m.put("isOwner", loginUser != null && loginUser.userId().equals(rec.getUserId()));
        return m;
    }

    private Map<String, Object> activeView(BargainActive a, boolean detail) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("goodsId", a.getGoodsId());
        m.put("floorPrice", a.getFloorPrice());
        m.put("validHours", a.getValidHours());
        m.put("helpLimit", a.getHelpLimit());
        m.put("startTime", a.getStartTime());
        m.put("endTime", a.getEndTime());
        m.put("status", a.getStatus());
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
            List<String> images = new com.fasterxml.jackson.databind.ObjectMapper().readValue(imagesJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            return images.isEmpty() ? "" : images.get(0);
        } catch (Exception e) {
            return "";
        }
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser.userId();
    }

    public record StartRequest(Long activeId, Long skuId) {
    }
}
