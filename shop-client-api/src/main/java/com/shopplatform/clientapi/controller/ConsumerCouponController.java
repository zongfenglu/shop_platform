package com.shopplatform.clientapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.entity.Coupon;
import com.shopplatform.domain.marketing.entity.UserCoupon;
import com.shopplatform.domain.marketing.service.CouponService;
import com.shopplatform.domain.marketing.service.CouponSnapshot;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.framework.security.LoginUserContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消费者端优惠券：领券中心、领券、我的优惠券。
 * 对应原型 h5/coupon-center.html 与"我的-优惠券"入口。
 */
@RestController
@RequestMapping("/api/coupons")
public class ConsumerCouponController {

    private final CouponService couponService;
    private final UserCouponService userCouponService;
    private final ObjectMapper objectMapper;

    public ConsumerCouponController(CouponService couponService,
                                     UserCouponService userCouponService,
                                     ObjectMapper objectMapper) {
        this.couponService = couponService;
        this.userCouponService = userCouponService;
        this.objectMapper = objectMapper;
    }

    /** 领券中心：当前可领取的券。 */
    @GetMapping("/receivable")
    public Result<List<Coupon>> receivable() {
        return Result.ok(couponService.listReceivable());
    }

    /** 领取一张券。 */
    @PostMapping("/{couponId}/receive")
    public Result<UserCoupon> receive(@PathVariable Long couponId) {
        Long userId = requireLoginUserId();
        return Result.ok(userCouponService.receive(userId, couponId));
    }

    /** 我的优惠券，status 可为 null(全部)/unused/used/expired。 */
    @GetMapping("/mine")
    public Result<List<Map<String, Object>>> mine(@RequestParam(required = false) String status) {
        Long userId = requireLoginUserId();
        List<UserCoupon> list = userCouponService.listMyCoupons(userId, status);
        // 把 snapshot 解析出来一起返回，前端无需再二次请求券模板
        return Result.ok(list.stream().map(uc -> {
            Map<String, Object> view = new HashMap<>();
            view.put("id", uc.getId());
            view.put("couponId", uc.getCouponId());
            view.put("status", uc.getStatus());
            view.put("startTime", uc.getStartTime());
            view.put("endTime", uc.getEndTime());
            view.put("useOrderId", uc.getUseOrderId());
            view.put("snapshot", parseSnapshot(uc.getSnapshot()));
            return view;
        }).toList());
    }

    private CouponSnapshot parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CouponSnapshot.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser.userId();
    }
}
