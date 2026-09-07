package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.marketing.entity.SignConfig;
import com.shopplatform.domain.marketing.entity.SignRecord;
import com.shopplatform.domain.marketing.mapper.SignRecordMapper;
import com.shopplatform.domain.marketing.service.ContinuousRule;
import com.shopplatform.domain.marketing.service.SignConfigService;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignRecordServiceImplTest {

    private SignConfigService signConfigService;
    private MemberService memberService;
    private UserCouponService userCouponService;
    private SignRecordMapper signRecordMapper;
    private SignRecordServiceImpl signRecordService;

    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_SHOP_ID = 999L;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SignRecord.class);
    }

    @BeforeEach
    void setUp() {
        signConfigService = mock(SignConfigService.class);
        memberService = mock(MemberService.class);
        userCouponService = mock(UserCouponService.class);
        signRecordMapper = mock(SignRecordMapper.class);

        SignRecordServiceImpl impl = new SignRecordServiceImpl(signConfigService, memberService, userCouponService);
        ReflectionTestUtils.setField(impl, "baseMapper", signRecordMapper);
        signRecordService = spy(impl);

        TenantContext.set(TEST_SHOP_ID);

        // 默认签到配置
        SignConfig config = new SignConfig();
        config.setId(1L);
        config.setShopId(TEST_SHOP_ID);
        config.setDailyPoints(2);
        config.setContinuousRules("[]");
        when(signConfigService.getOrCreate()).thenReturn(config);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---- dailySign ----

    @Test
    void dailySign_firstDay_streakStartsAt1() {
        // 没有任何签到记录
        when(signRecordService.count(any())).thenReturn(0L);
        when(signRecordService.getOne(any())).thenReturn(null);
        doNothing().when(memberService).adjustPoints(eq(TEST_USER_ID), anyInt(), anyString(), anyString());

        Map<String, Object> result = signRecordService.dailySign(TEST_USER_ID);

        assertEquals(2, result.get("earnedPoints"));
        assertEquals(1, result.get("dayNumber"));
        verify(memberService).adjustPoints(TEST_USER_ID, 2, "sign", "每日签到");
    }

    @Test
    void dailySign_continuesStreak() {
        // 昨天已签到，连续5天
        SignRecord yesterday = new SignRecord();
        yesterday.setUserId(TEST_USER_ID);
        yesterday.setSignDate(LocalDate.now().minusDays(1));
        yesterday.setDayNumber(5);

        when(signRecordService.count(any())).thenReturn(0L);
        when(signRecordService.getOne(any())).thenReturn(yesterday);
        doNothing().when(memberService).adjustPoints(eq(TEST_USER_ID), anyInt(), anyString(), anyString());

        Map<String, Object> result = signRecordService.dailySign(TEST_USER_ID);

        assertEquals(6, result.get("dayNumber"));
    }

    @Test
    void dailySign_streakBroken_restarts() {
        // 昨天没有签到记录 → 断签，dayNumber 从 1 重新开始
        when(signRecordService.count(any())).thenReturn(0L);
        when(signRecordService.getOne(any())).thenReturn(null);
        doNothing().when(memberService).adjustPoints(eq(TEST_USER_ID), anyInt(), anyString(), anyString());

        Map<String, Object> result = signRecordService.dailySign(TEST_USER_ID);

        assertEquals(1, result.get("dayNumber"));
    }

    @Test
    void dailySign_alreadySigned_throws() {
        // 今天已有记录
        when(signRecordService.count(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> signRecordService.dailySign(TEST_USER_ID));
    }

    @Test
    void dailySign_milestonePoints_awarded() {
        // 连续3天奖励：额外+5积分
        SignConfig config = new SignConfig();
        config.setId(1L);
        config.setShopId(TEST_SHOP_ID);
        config.setDailyPoints(2);
        config.setContinuousRules("[{\"days\":3,\"type\":\"points\",\"value\":5}]");
        when(signConfigService.getOrCreate()).thenReturn(config);

        SignRecord yesterday = new SignRecord();
        yesterday.setUserId(TEST_USER_ID);
        yesterday.setSignDate(LocalDate.now().minusDays(1));
        yesterday.setDayNumber(2);

        when(signRecordService.count(any())).thenReturn(0L);
        when(signRecordService.getOne(any())).thenReturn(yesterday);
        doNothing().when(memberService).adjustPoints(eq(TEST_USER_ID), anyInt(), anyString(), anyString());

        Map<String, Object> result = signRecordService.dailySign(TEST_USER_ID);

        assertEquals(7, result.get("earnedPoints")); // 2 + 5
        assertEquals(3, result.get("dayNumber"));
        verify(memberService).adjustPoints(TEST_USER_ID, 5, "sign", "连续签到3天奖励");
    }

    @Test
    void dailySign_milestoneCoupon_issuesUserCoupon() {
        // 连续7天奖励：发券
        SignConfig config = new SignConfig();
        config.setId(1L);
        config.setShopId(TEST_SHOP_ID);
        config.setDailyPoints(2);
        config.setContinuousRules("[{\"days\":7,\"type\":\"coupon\",\"couponId\":123}]");
        when(signConfigService.getOrCreate()).thenReturn(config);

        SignRecord yesterday = new SignRecord();
        yesterday.setUserId(TEST_USER_ID);
        yesterday.setSignDate(LocalDate.now().minusDays(1));
        yesterday.setDayNumber(6);

        when(signRecordService.count(any())).thenReturn(0L);
        when(signRecordService.getOne(any())).thenReturn(yesterday);
        doNothing().when(memberService).adjustPoints(eq(TEST_USER_ID), anyInt(), anyString(), anyString());

        Map<String, Object> result = signRecordService.dailySign(TEST_USER_ID);

        assertEquals(7, result.get("dayNumber"));
        assertEquals(123L, result.get("userCouponId"));
        verify(userCouponService).issueForReward(TEST_USER_ID, 123L);
    }

    @Test
    void dailySign_milestoneCoupon_couponMissing_skips() {
        // couponId=null 时不抛异常，仅跳过
        SignConfig config = new SignConfig();
        config.setId(1L);
        config.setShopId(TEST_SHOP_ID);
        config.setDailyPoints(2);
        config.setContinuousRules("[{\"days\":3,\"type\":\"coupon\",\"couponId\":null}]");
        when(signConfigService.getOrCreate()).thenReturn(config);

        SignRecord yesterday = new SignRecord();
        yesterday.setUserId(TEST_USER_ID);
        yesterday.setSignDate(LocalDate.now().minusDays(1));
        yesterday.setDayNumber(2);

        when(signRecordService.count(any())).thenReturn(0L);
        when(signRecordService.getOne(any())).thenReturn(yesterday);
        doNothing().when(memberService).adjustPoints(eq(TEST_USER_ID), anyInt(), anyString(), anyString());

        Map<String, Object> result = signRecordService.dailySign(TEST_USER_ID);

        assertEquals(3, result.get("dayNumber"));
        // issueForReward 不应被调用
        verify(userCouponService, never()).issueForReward(anyLong(), anyLong());
    }

    // ---- makeupSign ----

    @Test
    void makeupSign_restoresStreak() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        // day-before-yesterday 连续3天
        SignRecord dayBefore = new SignRecord();
        dayBefore.setUserId(TEST_USER_ID);
        dayBefore.setSignDate(yesterday.minusDays(1));
        dayBefore.setDayNumber(3);

        when(signRecordService.count(any())).thenReturn(0L);
        when(signRecordService.getOne(any())).thenReturn(dayBefore);

        Map<String, Object> result = signRecordService.makeupSign(TEST_USER_ID, yesterday);

        assertEquals(0, result.get("earnedPoints"));
        assertEquals(4, result.get("dayNumber"));
    }

    @Test
    void makeupSign_invalidDate_throws() {
        // 不是昨天
        assertThrows(BusinessException.class,
                () -> signRecordService.makeupSign(TEST_USER_ID, LocalDate.now().minusDays(2)));
    }
}
