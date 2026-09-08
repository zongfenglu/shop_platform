package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.marketing.entity.Coupon;
import com.shopplatform.domain.marketing.entity.UserCoupon;
import com.shopplatform.domain.marketing.mapper.UserCouponMapper;
import com.shopplatform.domain.marketing.service.CouponService;
import com.shopplatform.domain.marketing.service.CouponSnapshot;
import com.shopplatform.domain.marketing.service.UserCouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements UserCouponService {

    private final CouponService couponService;
    private final UserCouponMapper userCouponMapper;
    private final ObjectMapper objectMapper;

    public UserCouponServiceImpl(CouponService couponService,
                                  UserCouponMapper userCouponMapper,
                                  ObjectMapper objectMapper) {
        this.couponService = couponService;
        this.userCouponMapper = userCouponMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UserCoupon receive(Long userId, Long couponId) {
        Coupon coupon = couponService.getByIdWithTenant(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        if (!"on".equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        // fixed 类型必须在领取时间窗内；receive 类型领取后才开始计时，这里不卡时间。
        if ("fixed".equals(coupon.getExpireType())) {
            if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券尚未开放领取");
            }
            if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券领取已结束");
            }
        }

        // 每人限领校验：统计该用户已领取的本券数量（含已使用/已过期），避免反复领取刷量。
        long owned = count(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        int limit = coupon.getLimitPerUser() == null ? 0 : coupon.getLimitPerUser();
        if (limit > 0 && owned >= limit) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "已达领取上限");
        }

        // 原子扣减库存：total_num=0 表示不限；返回 0 说明已发完，回滚事务。
        if (userCouponMapper.incrReceived(couponId) == 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券已领完");
        }

        return buildUserCoupon(userId, coupon, now);
    }

    @Override
    public boolean tryUse(Long userCouponId, Long orderId) {
        return userCouponMapper.markUsed(userCouponId, orderId) > 0;
    }

    @Override
    public void release(Long userCouponId, Long orderId) {
        userCouponMapper.releaseUsed(userCouponId, orderId);
    }

    @Override
    public List<UserCoupon> listMyCoupons(Long userId, String status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        return list(wrapper);
    }

    @Override
    public int expireOverdue() {
        return userCouponMapper.expireOverdue(LocalDateTime.now());
    }

    @Override
    @Transactional
    public UserCoupon issueForReward(Long userId, Long couponId) {
        Coupon coupon = couponService.getByIdWithTenant(couponId);
        if (coupon == null || !"on".equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券不存在或已下架");
        }
        // 奖励/兑换渠道跳过库存扣减和限领校验，直接发券
        return buildUserCoupon(userId, coupon, LocalDateTime.now());
    }

    // ---- 私有工具方法 ----

    /** 计算有效期并写入 user_coupon 记录。receipt/receive 均复用此逻辑。 */
    private UserCoupon buildUserCoupon(Long userId, Coupon coupon, LocalDateTime now) {
        LocalDateTime start;
        LocalDateTime end;
        if ("receive".equals(coupon.getExpireType()) && coupon.getExpireDays() != null) {
            start = now;
            end = now.plusDays(coupon.getExpireDays());
        } else {
            start = coupon.getStartTime();
            end = coupon.getEndTime();
        }

        UserCoupon uc = new UserCoupon();
        uc.setShopId(coupon.getShopId());
        uc.setUserId(userId);
        uc.setCouponId(coupon.getId());
        uc.setStatus("unused");
        uc.setStartTime(start);
        uc.setEndTime(end);
        uc.setSnapshot(buildSnapshot(coupon));
        save(uc);
        return uc;
    }

    private String buildSnapshot(Coupon coupon) {
        try {
            CouponSnapshot snap = new CouponSnapshot(
                    coupon.getName(),
                    coupon.getType(),
                    coupon.getReducePrice(),
                    coupon.getDiscountRatio(),
                    coupon.getMinPrice(),
                    coupon.getApplyRange(),
                    parseIdList(coupon.getApplyRangeConfig()));
            return objectMapper.writeValueAsString(snap);
        } catch (Exception e) {
            throw new IllegalStateException("序列化券快照失败: " + coupon.getId(), e);
        }
    }

    private List<Long> parseIdList(String json) {
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
}
