package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.Coupon;
import com.shopplatform.domain.marketing.mapper.CouponMapper;
import com.shopplatform.domain.marketing.service.CouponService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    @Override
    public List<Coupon> listReceivable() {
        LocalDateTime now = LocalDateTime.now();
        // status=on；fixed 类型要求 now 在 [start_time, end_time] 内；receive 类型不卡领取时间窗（领后才开始计时）
        return list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, "on")
                .and(w -> w.isNull(Coupon::getExpireType).or().ne(Coupon::getExpireType, "fixed")
                        .or().le(Coupon::getStartTime, now).ge(Coupon::getEndTime, now))
                .orderByDesc(Coupon::getId));
    }
}
