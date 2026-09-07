package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.entity.UserBalanceLog;
import com.shopplatform.domain.member.entity.UserPointsLog;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.UserBalanceLogService;
import com.shopplatform.domain.member.service.UserPointsLogService;
import com.shopplatform.storeapi.dto.AdjustAssetRequest;
import com.shopplatform.storeapi.dto.AdjustPointsRequest;
import com.shopplatform.storeapi.dto.MemberListQuery;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/** 会员基础资料与资产管理，对应 docs/prototype/store/member-list.html。 */
@RestController
@RequestMapping("/store/members")
public class StoreMemberController {

    private final MemberService memberService;
    private final UserBalanceLogService balanceLogService;
    private final UserPointsLogService pointsLogService;

    public StoreMemberController(MemberService memberService,
                                  UserBalanceLogService balanceLogService,
                                  UserPointsLogService pointsLogService) {
        this.memberService = memberService;
        this.balanceLogService = balanceLogService;
        this.pointsLogService = pointsLogService;
    }

    @GetMapping
    public Result<IPage<Member>> list(MemberListQuery query) {
        var wrapper = Wrappers.<Member>lambdaQuery();
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(Member::getNickname, query.keyword())
                    .or().like(Member::getMobile, query.keyword()));
        }
        if (query.status() != null) {
            wrapper.eq(Member::getStatus, query.status());
        }
        wrapper.orderByDesc(Member::getCreateTime);
        return Result.ok(memberService.page(new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault()), wrapper));
    }

    /** 会员详情：含余额/积分/成长值/等级/消费统计等资产字段（V12 已补齐）。 */
    @GetMapping("/{id}")
    public Result<Member> detail(@PathVariable Long id) {
        return Result.ok(memberService.getByIdWithTenant(id));
    }

    /** 余额变动明细。 */
    @GetMapping("/{id}/balance-logs")
    public Result<IPage<UserBalanceLog>> balanceLogs(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        memberService.getByIdWithTenant(id);
        return Result.ok(balanceLogService.pageByUser(id, Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)));
    }

    /** 积分变动明细。 */
    @GetMapping("/{id}/points-logs")
    public Result<IPage<UserPointsLog>> pointsLogs(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
        memberService.getByIdWithTenant(id);
        return Result.ok(pointsLogService.pageByUser(id, Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)));
    }

    /** 后台调整余额（正数充值/负数扣减），写一条 scene=admin 的余额流水。 */
    @PostMapping("/{id}/adjust-balance")
    public Result<Void> adjustBalance(@PathVariable Long id, @Valid @RequestBody AdjustAssetRequest req) {
        // 路径 id 与请求体 userId 必须一致，防止前端构造错位请求绕过归属校验
        if (!id.equals(req.userId())) {
            return Result.fail(10001, "会员不一致");
        }
        memberService.adjustBalance(id, req.amount(), "admin", req.remark(), null);
        return Result.ok();
    }

    /** 后台调整积分，写一条 scene=admin 的积分流水。 */
    @PostMapping("/{id}/adjust-points")
    public Result<Void> adjustPoints(@PathVariable Long id, @Valid @RequestBody AdjustPointsRequest req) {
        if (!id.equals(req.userId())) {
            return Result.fail(10001, "会员不一致");
        }
        memberService.adjustPoints(id, req.value(), "admin", req.remark());
        return Result.ok();
    }
}
