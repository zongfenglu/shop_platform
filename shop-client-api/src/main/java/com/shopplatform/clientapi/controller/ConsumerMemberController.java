package com.shopplatform.clientapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.entity.UserBalanceLog;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.entity.UserPointsLog;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.UserBalanceLogService;
import com.shopplatform.domain.member.service.UserGradeService;
import com.shopplatform.domain.member.service.UserPointsLogService;
import com.shopplatform.framework.security.LoginUserContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消费者端"我的"资产：个人资料（含余额/积分/成长值/等级）、余额明细、积分明细、会员等级说明。
 * 对应原型 h5/my.html。只能看自己——所有查询都按 LoginUserContext 的 userId 过滤。
 */
@RestController
@RequestMapping("/api/member")
public class ConsumerMemberController {

    private final MemberService memberService;
    private final UserBalanceLogService balanceLogService;
    private final UserPointsLogService pointsLogService;
    private final UserGradeService userGradeService;

    public ConsumerMemberController(MemberService memberService,
                                      UserBalanceLogService balanceLogService,
                                      UserPointsLogService pointsLogService,
                                      UserGradeService userGradeService) {
        this.memberService = memberService;
        this.balanceLogService = balanceLogService;
        this.pointsLogService = pointsLogService;
        this.userGradeService = userGradeService;
    }

    /** 我的资料与资产（余额/积分/成长值/等级/消费统计）。 */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Long userId = requireLoginUserId();
        Member member = memberService.getByIdWithTenant(userId);
        UserGrade grade = member.getGradeId() == null ? null : findGrade(member.getGradeId());
        return Result.ok(Map.of("member", member, "grade", grade == null ? Map.of() : grade));
    }

    /** 我的余额变动明细。 */
    @GetMapping("/balance-logs")
    public Result<IPage<UserBalanceLog>> balanceLogs(@RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = requireLoginUserId();
        return Result.ok(balanceLogService.pageByUser(userId, Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)));
    }

    /** 我的积分变动明细。 */
    @GetMapping("/points-logs")
    public Result<IPage<UserPointsLog>> pointsLogs(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = requireLoginUserId();
        return Result.ok(pointsLogService.pageByUser(userId, Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)));
    }

    /** 会员等级说明（全部等级 + 我的当前等级），供"会员等级页"展示升级路径。 */
    @GetMapping("/grades")
    public Result<Map<String, Object>> grades() {
        Long userId = requireLoginUserId();
        Member member = memberService.getByIdWithTenant(userId);
        List<UserGrade> grades = userGradeService.listAllOrdered();
        return Result.ok(Map.of("grades", grades, "currentGradeId", member.getGradeId() == null ? 0 : member.getGradeId(),
                "growthValue", member.getGrowthValue() == null ? 0 : member.getGrowthValue()));
    }

    private UserGrade findGrade(Long gradeId) {
        for (UserGrade g : userGradeService.listAllOrdered()) {
            if (g.getId().equals(gradeId)) {
                return g;
            }
        }
        return null;
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser.userId();
    }
}
