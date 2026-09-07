package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.marketing.entity.ExchangeRecord;
import com.shopplatform.domain.marketing.entity.PointsGoods;
import com.shopplatform.domain.marketing.entity.UserCoupon;
import com.shopplatform.domain.marketing.mapper.ExchangeRecordMapper;
import com.shopplatform.domain.marketing.mapper.PointsGoodsMapper;
import com.shopplatform.domain.marketing.service.PointsGoodsService;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExchangeRecordServiceImplTest {

    private PointsGoodsService pointsGoodsService;
    private PointsGoodsMapper pointsGoodsMapper;
    private MemberService memberService;
    private UserCouponService userCouponService;
    private ExchangeRecordMapper exchangeRecordMapper;
    private ExchangeRecordServiceImpl exchangeRecordService;

    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_SHOP_ID = 999L;
    private static final Long TEST_GOODS_ID = 10L;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExchangeRecord.class);
    }

    @BeforeEach
    void setUp() {
        pointsGoodsService = mock(PointsGoodsService.class);
        pointsGoodsMapper = mock(PointsGoodsMapper.class);
        memberService = mock(MemberService.class);
        userCouponService = mock(UserCouponService.class);
        exchangeRecordMapper = mock(ExchangeRecordMapper.class);

        ExchangeRecordServiceImpl impl = new ExchangeRecordServiceImpl(
                pointsGoodsService, pointsGoodsMapper, memberService, userCouponService);
        ReflectionTestUtils.setField(impl, "baseMapper", exchangeRecordMapper);
        exchangeRecordService = spy(impl);

        TenantContext.set(TEST_SHOP_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private PointsGoods pointsOnlyGoods() {
        PointsGoods g = new PointsGoods();
        g.setId(TEST_GOODS_ID);
        g.setShopId(TEST_SHOP_ID);
        g.setName("测试兑换商品");
        g.setPoints(300);
        g.setCash(BigDecimal.ZERO);
        g.setStock(10);
        g.setStatus("on");
        g.setSort(0);
        return g;
    }

    private PointsGoods couponGoods() {
        PointsGoods g = new PointsGoods();
        g.setId(TEST_GOODS_ID);
        g.setShopId(TEST_SHOP_ID);
        g.setName("优惠券兑换");
        g.setCouponId(123L);
        g.setPoints(150);
        g.setCash(BigDecimal.ZERO);
        g.setStock(0); // 不限
        g.setStatus("on");
        g.setSort(0);
        return g;
    }

    private Member memberWithPoints(int points) {
        Member m = new Member();
        m.setId(TEST_USER_ID);
        m.setPoints(points);
        return m;
    }

    // ---- create ----

    @Test
    void create_purePoints_validatesOnly() {
        doReturn(pointsOnlyGoods()).when(pointsGoodsService).getByIdWithTenant(TEST_GOODS_ID);
        doReturn(memberWithPoints(500)).when(memberService).getByIdWithTenant(TEST_USER_ID);

        ExchangeRecord record = exchangeRecordService.create(TEST_USER_ID, TEST_GOODS_ID);

        assertEquals("unpaid", record.getPayStatus());
        assertEquals(300, record.getPointsCost());
        // create 不应调用 adjustPoints 或 decrStock
        verify(memberService, never()).adjustPoints(anyLong(), anyInt(), anyString(), anyString());
        verify(pointsGoodsMapper, never()).decrStock(anyLong());
    }

    @Test
    void create_insufficientPoints_throws() {
        when(pointsGoodsService.getByIdWithTenant(TEST_GOODS_ID)).thenReturn(pointsOnlyGoods());
        when(memberService.getByIdWithTenant(TEST_USER_ID)).thenReturn(memberWithPoints(50));

        assertThrows(BusinessException.class,
                () -> exchangeRecordService.create(TEST_USER_ID, TEST_GOODS_ID));
    }

    // ---- pay ----

    @Test
    void pay_purePoints_deductsStockAndPoints() {
        PointsGoods goods = pointsOnlyGoods();
        when(pointsGoodsService.getByIdWithTenant(TEST_GOODS_ID)).thenReturn(goods);
        when(pointsGoodsMapper.decrStock(TEST_GOODS_ID)).thenReturn(1);

        ExchangeRecord record = new ExchangeRecord();
        record.setId(20L);
        record.setUserId(TEST_USER_ID);
        record.setPointsGoodsId(TEST_GOODS_ID);
        record.setGoodsType("goods");
        record.setPointsCost(300);
        record.setCashPrice(BigDecimal.ZERO);
        record.setPayStatus("unpaid");
        record.setStatus("pending");
        doReturn(record).when(exchangeRecordService).getByIdWithTenant(20L);

        doNothing().when(memberService).adjustPoints(eq(TEST_USER_ID), anyInt(), anyString(), anyString());

        ExchangeRecord result = exchangeRecordService.pay(TEST_USER_ID, 20L);

        assertEquals("paid", result.getPayStatus());
        verify(pointsGoodsMapper).decrStock(TEST_GOODS_ID);
        verify(memberService).adjustPoints(TEST_USER_ID, -300, "mall", "积分商城兑换");
    }

    @Test
    void pay_stockExhausted_throws() {
        PointsGoods goods = pointsOnlyGoods();
        goods.setStock(5);
        when(pointsGoodsService.getByIdWithTenant(TEST_GOODS_ID)).thenReturn(goods);
        when(pointsGoodsMapper.decrStock(TEST_GOODS_ID)).thenReturn(0); // 库存不足

        ExchangeRecord record = new ExchangeRecord();
        record.setId(20L);
        record.setUserId(TEST_USER_ID);
        record.setPointsGoodsId(TEST_GOODS_ID);
        record.setGoodsType("goods");
        record.setPointsCost(300);
        record.setCashPrice(BigDecimal.ZERO);
        record.setPayStatus("unpaid");
        doReturn(record).when(exchangeRecordService).getByIdWithTenant(20L);

        assertThrows(BusinessException.class,
                () -> exchangeRecordService.pay(TEST_USER_ID, 20L));
        // 积分扣减不应被调用（库存不足先抛异常）
        verify(memberService, never()).adjustPoints(anyLong(), anyInt(), anyString(), anyString());
    }

    @Test
    void pay_couponType_issuesUserCoupon() {
        PointsGoods goods = couponGoods();
        when(pointsGoodsService.getByIdWithTenant(TEST_GOODS_ID)).thenReturn(goods);
        when(pointsGoodsMapper.decrStock(TEST_GOODS_ID)).thenReturn(0); // stock=0 不限
        UserCoupon uc = new UserCoupon();
        uc.setId(50L);
        when(userCouponService.issueForReward(TEST_USER_ID, 123L)).thenReturn(uc);

        ExchangeRecord record = new ExchangeRecord();
        record.setId(21L);
        record.setUserId(TEST_USER_ID);
        record.setPointsGoodsId(TEST_GOODS_ID);
        record.setGoodsType("coupon");
        record.setCouponId(123L);
        record.setPointsCost(150);
        record.setCashPrice(BigDecimal.ZERO);
        record.setPayStatus("unpaid");
        doReturn(record).when(exchangeRecordService).getByIdWithTenant(21L);

        doNothing().when(memberService).adjustPoints(eq(TEST_USER_ID), anyInt(), anyString(), anyString());

        ExchangeRecord result = exchangeRecordService.pay(TEST_USER_ID, 21L);

        assertEquals("paid", result.getPayStatus());
        assertEquals(50L, result.getUserCouponId());
        verify(userCouponService).issueForReward(TEST_USER_ID, 123L);
    }

    @Test
    void pay_doublePay_rejected() {
        ExchangeRecord record = new ExchangeRecord();
        record.setId(22L);
        record.setUserId(TEST_USER_ID);
        record.setPayStatus("paid"); // 已支付
        doReturn(record).when(exchangeRecordService).getByIdWithTenant(22L);

        assertThrows(BusinessException.class,
                () -> exchangeRecordService.pay(TEST_USER_ID, 22L));
    }

    @Test
    void pay_otherUsersRecord_rejected() {
        ExchangeRecord record = new ExchangeRecord();
        record.setId(23L);
        record.setUserId(9999L); // 不属于当前用户
        record.setPayStatus("unpaid");
        doReturn(record).when(exchangeRecordService).getByIdWithTenant(23L);

        assertThrows(BusinessException.class,
                () -> exchangeRecordService.pay(TEST_USER_ID, 23L));
    }
}
