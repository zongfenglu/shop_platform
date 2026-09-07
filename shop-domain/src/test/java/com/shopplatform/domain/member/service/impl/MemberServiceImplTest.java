package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.mapper.MemberMapper;
import com.shopplatform.domain.member.service.UserBalanceLogService;
import com.shopplatform.domain.member.service.UserGradeService;
import com.shopplatform.domain.member.service.UserPointsLogService;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MemberServiceImpl} 会员资产核心逻辑单测：余额/积分原子调整（含透支拦截）、
 * 支付后成长值累计与等级重算。见开发计划 Sprint 7 验收标准。
 * <p>
 * 不拉 Spring 上下文，baseMapper 用反射注入 mock，{@code getByIdWithTenant} 在 spy 上打桩——
 * 与 OrderServiceImplTest 同一套路：只验证业务分支逻辑，落库行为由 Testcontainers 集成测试兜底。
 */
class MemberServiceImplTest {

    private MemberMapper memberMapper;
    private UserBalanceLogService balanceLogService;
    private UserPointsLogService pointsLogService;
    private UserGradeService userGradeService;
    private MemberServiceImpl memberService;

    @BeforeAll
    static void initLambdaCache() {
        // LambdaQueryWrapper 的方法引用依赖实体 lambda 缓存，无 Spring 上下文时需手工初始化
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Member.class);
    }

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        balanceLogService = mock(UserBalanceLogService.class);
        pointsLogService = mock(UserPointsLogService.class);
        userGradeService = mock(UserGradeService.class);

        MemberServiceImpl impl = new MemberServiceImpl(balanceLogService, pointsLogService, userGradeService);
        ReflectionTestUtils.setField(impl, "baseMapper", memberMapper);
        memberService = spy(impl);
        TenantContext.set(999L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void adjustBalance_recharge_writesLogWithBeforeAfterRecomputedFromNewBalance() {
        when(memberMapper.adjustBalance(eq(1L), eq(new BigDecimal("50.00")))).thenReturn(1);
        Member after = member(1L, new BigDecimal("150.00"), 0, null);
        doReturn(after).when(memberService).getByIdWithTenant(1L);

        memberService.adjustBalance(1L, new BigDecimal("50.00"), "recharge", "充值", null);

        verify(balanceLogService).save(argThat(log ->
                log.getUserId().equals(1L)
                        && log.getMoney().compareTo(new BigDecimal("50.00")) == 0
                        && log.getBefore().compareTo(new BigDecimal("100.00")) == 0
                        && log.getAfter().compareTo(new BigDecimal("150.00")) == 0
                        && "recharge".equals(log.getScene())));
    }

    @Test
    void adjustBalance_consumeInsufficient_throwsAndNeverWritesLog() {
        when(memberMapper.adjustBalance(eq(1L), eq(new BigDecimal("-200.00")))).thenReturn(0);
        doReturn(member(1L, new BigDecimal("100.00"), 0, null)).when(memberService).getByIdWithTenant(1L);

        assertThrows(BusinessException.class, () ->
                memberService.adjustBalance(1L, new BigDecimal("-200.00"), "consume", "消费", null));

        verify(balanceLogService, never()).save(any());
    }

    @Test
    void adjustPoints_consumeInsufficient_throws() {
        when(memberMapper.adjustPoints(eq(1L), eq(-500))).thenReturn(0);
        doReturn(member(1L, BigDecimal.ZERO, 100, null)).when(memberService).getByIdWithTenant(1L);

        assertThrows(BusinessException.class, () ->
                memberService.adjustPoints(1L, -500, "consume", "消费"));

        verify(pointsLogService, never()).save(any());
    }

    @Test
    void recordPayment_accumulatesGrowthAndUpgradesGrade() {
        Member member = member(1L, BigDecimal.ZERO, 0, null);
        member.setGrowthValue(1500);
        member.setGradeId(null);
        when(memberMapper.recordPayment(eq(1L), eq(new BigDecimal("1500.00")))).thenReturn(1);
        doReturn(member).when(memberService).getByIdWithTenant(1L);
        when(userGradeService.listAllOrdered()).thenReturn(List.of(
                grade(10L, 0, 0), grade(11L, 1, 1000)));

        memberService.recordPayment(1L, new BigDecimal("1500.00"));

        // 1 元 = 1 成长值：1500 元 → +1500 成长值
        verify(memberMapper).addGrowth(1L, 1500);
        // 成长值 1500 满足 g1(1000)，应升级到 g1
        verify(memberMapper).updateGrade(1L, 11L);
    }

    @Test
    void recordPayment_zeroAmount_isNoop() {
        memberService.recordPayment(1L, BigDecimal.ZERO);

        verifyNoInteractions(memberMapper);
        verifyNoInteractions(userGradeService);
    }

    @Test
    void matchGrade_picksHighestGradeWhoseGrowthValueIsMet() {
        List<UserGrade> grades = List.of(grade(1L, 0, 0), grade(2L, 1, 1000), grade(3L, 2, 5000));

        assertEquals(1L, MemberServiceImpl.matchGrade(grades, 0).getId());
        assertEquals(2L, MemberServiceImpl.matchGrade(grades, 2500).getId());
        assertEquals(3L, MemberServiceImpl.matchGrade(grades, 99999).getId());
        assertNull(MemberServiceImpl.matchGrade(List.of(), 100));
    }

    private Member member(Long id, BigDecimal balance, Integer points, Long gradeId) {
        Member m = new Member();
        m.setId(id);
        m.setBalance(balance);
        m.setPoints(points);
        m.setGradeId(gradeId);
        m.setGrowthValue(0);
        return m;
    }

    private UserGrade grade(Long id, int weight, int growthValue) {
        UserGrade g = new UserGrade();
        g.setId(id);
        g.setWeight(weight);
        g.setGrowthValue(growthValue);
        return g;
    }
}
