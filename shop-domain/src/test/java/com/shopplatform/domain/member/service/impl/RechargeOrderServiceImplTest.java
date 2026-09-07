package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.shopplatform.domain.member.entity.RechargeOrder;
import com.shopplatform.domain.member.entity.RechargePlan;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.RechargePlanService;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link RechargeOrderServiceImpl} 充值闭环单测：建单、支付入账（余额/积分/成长值）、
 * 重复回调幂等。见开发计划 Sprint 7。
 */
class RechargeOrderServiceImplTest {

    private RechargePlanService rechargePlanService;
    private MemberService memberService;
    private RechargeOrderServiceImpl rechargeOrderService;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RechargeOrder.class);
    }

    @BeforeEach
    void setUp() {
        rechargePlanService = mock(RechargePlanService.class);
        memberService = mock(MemberService.class);
        RechargeOrderServiceImpl impl = new RechargeOrderServiceImpl(rechargePlanService, memberService);
        // ServiceImpl 的 baseMapper 在无 Spring 时为 null，注入 mock 避免 NPE（markPaid 不直接用 baseMapper，但保持一致）
        ReflectionTestUtils.setField(impl, "baseMapper",
                mock(com.shopplatform.domain.member.mapper.RechargeOrderMapper.class));
        rechargeOrderService = spy(impl);
        TenantContext.set(999L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createOrder_snapshotsPlanMoneyAndGifts() {
        RechargePlan plan = new RechargePlan();
        plan.setId(7L);
        plan.setMoney(new BigDecimal("100.00"));
        plan.setGiftMoney(new BigDecimal("20.00"));
        plan.setGiftPoints(200);
        when(rechargePlanService.getByIdWithTenant(7L)).thenReturn(plan);
        doReturn(true).when(rechargeOrderService).save(any(RechargeOrder.class));

        RechargeOrder order = rechargeOrderService.createOrder(1L, 7L);

        assertEquals(new BigDecimal("100.00"), order.getPayPrice());
        assertEquals(new BigDecimal("20.00"), order.getGiftMoney());
        assertEquals(200, order.getGiftPoints());
        assertEquals("unpaid", order.getPayStatus());
        assertTrue(order.getOrderNo().startsWith("RC"));
    }

    @Test
    void markPaid_firstTime_creditsBalancePointsAndGrowth() {
        RechargeOrder order = unpaidOrder(1L, new BigDecimal("100.00"), new BigDecimal("20.00"), 200);
        doReturn(order).when(rechargeOrderService).getByIdWithTenant(1L);
        doReturn(true).when(rechargeOrderService).update(any());

        boolean ok = rechargeOrderService.markPaid(1L, "wx_txn_1", "wechat");

        assertTrue(ok);
        // 到账 = 实付 100 + 赠送 20 = 120
        verify(memberService).adjustBalance(eq(1L), eq(new BigDecimal("120.00")), eq("recharge"), anyString(), eq(1L));
        verify(memberService).adjustPoints(eq(1L), eq(200), eq("recharge"), anyString());
        verify(memberService).addGrowth(eq(1L), eq(120));
        verify(memberService).upgradeGrade(1L);
    }

    @Test
    void markPaid_alreadyPaid_isIdempotent_doesNotCreditAgain() {
        RechargeOrder order = unpaidOrder(1L, new BigDecimal("100.00"), BigDecimal.ZERO, 0);
        order.setPayStatus("paid");
        doReturn(order).when(rechargeOrderService).getByIdWithTenant(1L);

        boolean ok = rechargeOrderService.markPaid(1L, "wx_txn_1", "wechat");

        assertFalse(ok);
        verifyNoInteractions(memberService);
    }

    private RechargeOrder unpaidOrder(Long id, BigDecimal payPrice, BigDecimal giftMoney, int giftPoints) {
        RechargeOrder o = new RechargeOrder();
        o.setId(id);
        o.setUserId(1L);
        o.setPlanId(7L);
        o.setPayPrice(payPrice);
        o.setGiftMoney(giftMoney);
        o.setGiftPoints(giftPoints);
        o.setPayStatus("unpaid");
        return o;
    }
}
