package com.shopplatform.domain.member.service.impl;

import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.service.MemberGradeUpgradeService;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.UserGradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class MemberGradeUpgradeServiceImpl implements MemberGradeUpgradeService {

    private static final Logger log = LoggerFactory.getLogger(MemberGradeUpgradeServiceImpl.class);

    private final MemberService memberService;
    private final UserGradeService userGradeService;

    public MemberGradeUpgradeServiceImpl(MemberService memberService, UserGradeService userGradeService) {
        this.memberService = memberService;
        this.userGradeService = userGradeService;
    }

    @Override
    public int upgradeAllForCurrentTenant() {
        // 等级表只读一次，避免逐会员重复查询——批量任务里这是典型的 N+1 来源
        List<UserGrade> gradesAsc = userGradeService.listAllOrdered();
        List<Member> members = memberService.list();
        int changed = 0;
        for (Member m : members) {
            UserGrade target = MemberServiceImpl.matchGrade(gradesAsc, m.getGrowthValue() == null ? 0 : m.getGrowthValue());
            Long targetId = target == null ? null : target.getId();
            if (!Objects.equals(targetId, m.getGradeId())) {
                memberService.setGrade(m.getId(), targetId);
                changed++;
            }
        }
        log.info("会员等级升级完成 changed={} total={}", changed, members.size());
        return changed;
    }
}
