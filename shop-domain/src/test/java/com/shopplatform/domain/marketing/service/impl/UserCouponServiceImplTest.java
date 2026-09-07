package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.marketing.entity.Coupon;
import com.shopplatform.domain.marketing.entity.UserCoupon;
import com.shopplatform.domain.marketing.mapper.UserCouponMapper;
import com.shopplatform.domain.marketing.service.CouponService;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link UserCouponServiceImpl} 领券/核销/退回单测：限领校验、库存原子扣减、快照写入、
 * 核销幂等、退回仅限本订单。见 Sprint 8 验收标准。
 * <p>
 * 与 MemberServiceImplTest 同一套路：不拉 Spring 上下文，baseMapper 用反射注入 mock，
 * {@code getByIdWithTenant}/{@code count}/{@code save} 在 spy 上打桩。
 */
class UserCouponServiceImplTest {

    private CouponService couponService;
    private UserCouponMapper userCouponMapper;
    private UserCouponServiceImpl userCouponService;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserCoupon.class);
    }

    @BeforeEach
    void setUp() {
        couponService = mock(CouponService.class);
        userCouponMapper = mock(UserCouponMapper.class);
        UserCouponServiceImpl impl = new UserCouponServiceImpl(couponService, userCouponMapper, new ObjectMapper());
        ReflectionTestUtils.setField(impl, "baseMapper", userCouponMapper);
        userCouponService = spy(impl);
        TenantContext.set(999L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Coupon fixedCoupon(int total, int received, int limit) {
        Coupon c = new Coupon();
        c.setId(1L);
        c.setShopId(999L);
        c.setName("满100减10");
        c.setType("reduce");
        c.setReducePrice(new BigDecimal("10.00"));
        c.setMinPrice(new BigDecimal("100.00"));
        c.setExpireType("fixed");
        c.setStartTime(LocalDateTime.now().minusDays(1));
        c.setEndTime(LocalDateTime.now().plusDays(1));
        c.setTotalNum(total);
        c.setReceivedNum(received);
        c.setLimitPerUser(limit);
        c.setApplyRange("all");
        c.setStatus("on");
        return c;
    }

    @Test
    void receive_success_incrsStockAndWritesSnapshot() {
        Coupon coupon = fixedCoupon(100, 10, 1);
        when(couponService.getByIdWithTenant(1L)).thenReturn(coupon);
        when(userCouponMapper.incrReceived(1L)).thenReturn(1);
        doReturn(0L).when(userCouponService).count(any());
        doReturn(true).when(userCouponService).save(any(UserCoupon.class));

        UserCoupon uc = userCouponService.receive(100L, 1L);

        assertNotNull(uc);
        assertEquals("unused", uc.getStatus());
        assertEquals(100L, uc.getUserId());
        // 快照包含券面额信息
        assertTrue(uc.getSnapshot().contains("满100减10"));
        verify(userCouponMapper).incrReceived(1L);
    }

    @Test
    void receive_reachedPerUserLimit_throws() {
        Coupon coupon = fixedCoupon(100, 10, 1);
        when(couponService.getByIdWithTenant(1L)).thenReturn(coupon);
        doReturn(1L).when(userCouponService).count(any()); // 已领 1 张，达上限

        assertThrows(BusinessException.class, () -> userCouponService.receive(100L, 1L));
        verify(userCouponMapper, never()).incrReceived(any());
    }

    @Test
    void receive_stockExhausted_throwsAndNeverWritesUserCoupon() {
        Coupon coupon = fixedCoupon(100, 100, 5);
        when(couponService.getByIdWithTenant(1L)).thenReturn(coupon);
        when(userCouponMapper.incrReceived(1L)).thenReturn(0); // 已发完
        doReturn(0L).when(userCouponService).count(any());

        assertThrows(BusinessException.class, () -> userCouponService.receive(100L, 1L));
        verify(userCouponService, never()).save(any(UserCoupon.class));
    }

    @Test
    void receive_offlineCoupon_throws() {
        Coupon coupon = fixedCoupon(100, 10, 1);
        coupon.setStatus("off");
        when(couponService.getByIdWithTenant(1L)).thenReturn(coupon);

        assertThrows(BusinessException.class, () -> userCouponService.receive(100L, 1L));
    }

    @Test
    void receive_receiveType_expireDaysFromNow() {
        Coupon coupon = fixedCoupon(0, 0, 1);
        coupon.setExpireType("receive");
        coupon.setExpireDays(7);
        coupon.setStartTime(null);
        coupon.setEndTime(null);
        when(couponService.getByIdWithTenant(1L)).thenReturn(coupon);
        when(userCouponMapper.incrReceived(1L)).thenReturn(1);
        doReturn(0L).when(userCouponService).count(any());
        doReturn(true).when(userCouponService).save(any(UserCoupon.class));

        UserCoupon uc = userCouponService.receive(100L, 1L);

        assertNotNull(uc.getStartTime());
        assertNotNull(uc.getEndTime());
        // 领取后7天有效
        assertTrue(uc.getEndTime().isAfter(uc.getStartTime()));
    }

    @Test
    void tryUse_unusedCoupon_marksUsed() {
        when(userCouponMapper.markUsed(1L, 55L)).thenReturn(1);
        assertTrue(userCouponService.tryUse(1L, 55L));
    }

    @Test
    void tryUse_alreadyUsed_returnsFalse() {
        when(userCouponMapper.markUsed(1L, 55L)).thenReturn(0);
        assertFalse(userCouponService.tryUse(1L, 55L));
    }

    @Test
    void release_delegatesToMapperWithOrderIdGuard() {
        userCouponService.release(1L, 55L);
        verify(userCouponMapper).releaseUsed(1L, 55L);
    }

    @Test
    void expireOverdue_delegatesToMapper() {
        when(userCouponMapper.expireOverdue(any())).thenReturn(7);
        assertEquals(7, userCouponService.expireOverdue());
    }
}
