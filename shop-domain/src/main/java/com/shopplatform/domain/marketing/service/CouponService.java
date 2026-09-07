package com.shopplatform.domain.marketing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shopplatform.domain.marketing.entity.Coupon;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface CouponService extends TenantSafeService<Coupon> {

    /** 领券中心展示的可用券：status=on 且（fixed 类型当前在有效期内 / receive 类型） */
    List<Coupon> listReceivable();
}
