package com.shopplatform.storeapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.entity.ExchangeRecord;
import com.shopplatform.domain.marketing.entity.PointsGoods;
import com.shopplatform.domain.marketing.service.ExchangeRecordService;
import com.shopplatform.domain.marketing.service.PointsGoodsService;
import com.shopplatform.storeapi.dto.SavePointsGoodsRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积分商城管理。对应原型 store 营销中心-积分商城。
 * CRUD 兑换项 + 兑换记录查询（发货队列）。
 */
@RestController
@RequestMapping("/store/points-goods")
public class StorePointsGoodsController {

    private final PointsGoodsService pointsGoodsService;
    private final ExchangeRecordService exchangeRecordService;

    public StorePointsGoodsController(PointsGoodsService pointsGoodsService,
                                       ExchangeRecordService exchangeRecordService) {
        this.pointsGoodsService = pointsGoodsService;
        this.exchangeRecordService = exchangeRecordService;
    }

    @GetMapping
    public Result<List<PointsGoods>> list() {
        return Result.ok(pointsGoodsService.listAll());
    }

    @PostMapping
    public Result<PointsGoods> create(@Valid @RequestBody SavePointsGoodsRequest req) {
        validateOneOf(req.goodsId(), req.couponId());
        PointsGoods entity = new PointsGoods();
        apply(req, entity);
        pointsGoodsService.save(entity);
        return Result.ok(entity);
    }

    @PutMapping("/{id}")
    public Result<PointsGoods> update(@PathVariable Long id, @Valid @RequestBody SavePointsGoodsRequest req) {
        validateOneOf(req.goodsId(), req.couponId());
        PointsGoods entity = pointsGoodsService.getByIdWithTenant(id);
        apply(req, entity);
        pointsGoodsService.updateById(entity);
        return Result.ok(entity);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pointsGoodsService.getByIdWithTenant(id);
        pointsGoodsService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/exchanges")
    public Result<List<ExchangeRecord>> listExchanges(@RequestParam(required = false) String status) {
        return Result.ok(exchangeRecordService.listByShop(status));
    }

    private void apply(SavePointsGoodsRequest req, PointsGoods entity) {
        entity.setName(req.name());
        entity.setImage(req.image());
        entity.setGoodsId(req.goodsId());
        entity.setCouponId(req.couponId());
        entity.setPoints(req.points());
        entity.setCash(req.cash() == null ? BigDecimal.ZERO : req.cash());
        entity.setStock(req.stock() == null ? 0 : req.stock());
        entity.setStatus(req.status() == null ? "on" : req.status());
        entity.setSort(req.sort() == null ? 0 : req.sort());
    }

    private void validateOneOf(Long goodsId, Long couponId) {
        boolean hasGoods = goodsId != null;
        boolean hasCoupon = couponId != null;
        if (hasGoods == hasCoupon) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "goodsId 和 couponId 必须且只能填一个");
        }
    }
}
