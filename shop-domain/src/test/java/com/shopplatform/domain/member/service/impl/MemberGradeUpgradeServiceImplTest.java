package com.shopplatform.domain.member.service.impl;

import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.UserGradeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * {@link MemberGradeUpgradeServiceImpl} 批量等级升级单测。见开发计划 Sprint 7"会员等级自动升级定时任务"。
 * 等级按 weight 升序：g0(id=1,growth=0) / g1(id=2,growth=1000) / g2(id=3,growth=5000)。
 */
class MemberGradeUpgradeServiceImplTest {

    @Test
    void upgradeAll_onlyUpdatesMembersWhoseGradeChanged() {
        MemberService memberService = mock(MemberService.class);
        UserGradeService userGradeService = mock(UserGradeService.class);
        MemberGradeUpgradeServiceImpl svc = new MemberGradeUpgradeServiceImpl(memberService, userGradeService);

        when(userGradeService.listAllOrdered()).thenReturn(List.of(
                grade(1L, 0, 0), grade(2L, 1, 1000), grade(3L, 2, 5000)));

        Member keep = member(10L, 1500, 2L);       // 1500 满足 g1(1000) 不满足 g2(5000) → 目标 g1(2)，已是 g1，不变
        Member promote = member(11L, 6000, 2L);     // 6000 满足 g2(5000) → 目标 g2(3)，从 g1 升级
        Member fresh = member(12L, 100, null);      // 100 仅满足 g0(0) → 目标 g0(1)，从 null 升级
        when(memberService.list()).thenReturn(List.of(keep, promote, fresh));

        int changed = svc.upgradeAllForCurrentTenant();

        assertEquals(2, changed);
        verify(memberService).setGrade(11L, 3L);
        verify(memberService).setGrade(12L, 1L);
        verify(memberService, never()).setGrade(eq(10L), any());
    }

    @Test
    void upgradeAll_emptyGrades_setsAllToNull() {
        MemberService memberService = mock(MemberService.class);
        UserGradeService userGradeService = mock(UserGradeService.class);
        MemberGradeUpgradeServiceImpl svc = new MemberGradeUpgradeServiceImpl(memberService, userGradeService);

        when(userGradeService.listAllOrdered()).thenReturn(List.of());
        Member m = member(10L, 5000, 2L);
        when(memberService.list()).thenReturn(List.of(m));

        int changed = svc.upgradeAllForCurrentTenant();

        assertEquals(1, changed);
        verify(memberService).setGrade(10L, null);
    }

    private Member member(Long id, int growth, Long gradeId) {
        Member m = new Member();
        m.setId(id);
        m.setGrowthValue(growth);
        m.setGradeId(gradeId);
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
