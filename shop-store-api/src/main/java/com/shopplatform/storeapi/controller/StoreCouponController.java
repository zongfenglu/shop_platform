package com.shopplatform.storeapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.entity.Coupon;
import com.shopplatform.domain.marketing.service.CouponService;
import com.shopplatform.storeapi.dto.SaveCouponRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** 优惠券管理：新建/编辑/上下架/删除。对应原型 store 营销中心-优惠券。 */
@RestController
@RequestMapping("/store/coupons")
public class StoreCouponController {

    private final CouponService couponService;
    private final ObjectMapper objectMapper;

    public StoreCouponController(CouponService couponService, ObjectMapper objectMapper) {
        this.couponService = couponService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Result<List<Coupon>> list() {
        return Result.ok(couponService.list());
    }

    @PostMapping
    public Result<Coupon> create(@Valid @RequestBody SaveCouponRequest req) {
        Coupon coupon = new Coupon();
        apply(req, coupon);
        if (coupon.getReceivedNum() == null) {
            coupon.setReceivedNum(0);
        }
        couponService.save(coupon);
        return Result.ok(coupon);
    }

    @PutMapping("/{id}")
    public Result<Coupon> update(@PathVariable Long id, @Valid @RequestBody SaveCouponRequest req) {
        Coupon coupon = couponService.getByIdWithTenant(id);
        apply(req, coupon);
        couponService.updateById(coupon);
        return Result.ok(coupon);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.getByIdWithTenant(id);
        couponService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveCouponRequest req, Coupon coupon) {
        coupon.setName(req.name());
        coupon.setType(req.type());
        coupon.setReducePrice(req.reducePrice());
        coupon.setDiscountRatio(req.discountRatio());
        coupon.setMinPrice(req.minPrice() == null ? BigDecimal.ZERO : req.minPrice());
        coupon.setExpireType(req.expireType());
        coupon.setStartTime(req.startTime());
        coupon.setEndTime(req.endTime());
        coupon.setExpireDays(req.expireDays());
        coupon.setTotalNum(req.totalNum() == null ? 0 : req.totalNum());
        coupon.setLimitPerUser(req.limitPerUser() == null ? 0 : Math.max(0, req.limitPerUser()));
        coupon.setApplyRange(req.applyRange() == null ? "all" : req.applyRange());
        coupon.setApplyRangeConfig(toJson(req.applyRangeConfig()));
        coupon.setStatus(req.status() == null ? "on" : req.status());
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化适用范围失败");
        }
    }
}
