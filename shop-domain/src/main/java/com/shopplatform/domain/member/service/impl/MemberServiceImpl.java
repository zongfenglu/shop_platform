package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.entity.UserBalanceLog;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.entity.UserPointsLog;
import com.shopplatform.domain.member.mapper.MemberMapper;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.member.service.UserBalanceLogService;
import com.shopplatform.domain.member.service.UserGradeService;
import com.shopplatform.domain.member.service.UserPointsLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    private final UserBalanceLogService balanceLogService;
    private final UserPointsLogService pointsLogService;
    private final UserGradeService userGradeService;

    public MemberServiceImpl(UserBalanceLogService balanceLogService,
                              UserPointsLogService pointsLogService,
                              UserGradeService userGradeService) {
        this.balanceLogService = balanceLogService;
        this.pointsLogService = pointsLogService;
        this.userGradeService = userGradeService;
    }

    @Override
    public Member loginOrRegister(String mobile) {
        if (!StringUtils.hasText(mobile) || !mobile.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "手机号格式不正确");
        }
        Member existing = this.getOne(Wrappers.<Member>lambdaQuery().eq(Member::getMobile, mobile));
        if (existing != null) {
            baseMapper.updateLastLoginTime(existing.getId(), LocalDateTime.now());
            return existing;
        }
        Member member = new Member();
        member.setMobile(mobile);
        member.setNickname("用户" + mobile.substring(7));
        member.setStatus(1);
        member.setBalance(BigDecimal.ZERO);
        member.setPoints(0);
        member.setGrowthValue(0);
        member.setPayMoney(BigDecimal.ZERO);
        member.setPayCount(0);
        member.setIsBlack(0);
        this.save(member);
        return member;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustBalance(Long userId, BigDecimal delta, String scene, String remark, Long orderId) {
        if (delta == null || delta.signum() == 0) {
            return;
        }
        int rows = baseMapper.adjustBalance(userId, delta);
        if (rows == 0) {
            // 0 行有两种可能：跨租户/不存在（getByIdWithTenant 会抛 403/404），或余额不足。
            // 先做归属校验，把"越权"和"余额不足"区分开，避免跨租户误报成"余额不足"。
            this.getByIdWithTenant(userId);
            throw new BusinessException(ErrorCode.PARAM_INVALID, "余额不足");
        }
        Member after = this.getByIdWithTenant(userId);
        UserBalanceLog log = new UserBalanceLog();
        log.setUserId(userId);
        log.setScene(scene);
        log.setMoney(delta);
        log.setBefore(after.getBalance().subtract(delta));
        log.setAfter(after.getBalance());
        log.setRemark(remark);
        log.setOrderId(orderId);
        balanceLogService.save(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(Long userId, int delta, String scene, String remark) {
        if (delta == 0) {
            return;
        }
        int rows = baseMapper.adjustPoints(userId, delta);
        if (rows == 0) {
            this.getByIdWithTenant(userId);
            throw new BusinessException(ErrorCode.PARAM_INVALID, "积分不足");
        }
        Member after = this.getByIdWithTenant(userId);
        UserPointsLog log = new UserPointsLog();
        log.setUserId(userId);
        log.setScene(scene);
        log.setValue(delta);
        log.setBefore(after.getPoints() - delta);
        log.setAfter(after.getPoints());
        log.setRemark(remark);
        pointsLogService.save(log);
    }

    @Override
    public void addGrowth(Long userId, int delta) {
        if (delta == 0) {
            return;
        }
        baseMapper.addGrowth(userId, delta);
    }

    @Override
    public void recordPayment(Long userId, BigDecimal payMoney) {
        if (payMoney == null || payMoney.signum() <= 0) {
            return;
        }
        baseMapper.recordPayment(userId, payMoney);
        // 1 元 = 1 成长值，截断小数（与"按消费金额升级"的常见口径一致）
        int growth = payMoney.intValue();
        if (growth > 0) {
            baseMapper.addGrowth(userId, growth);
        }
        upgradeGrade(userId);
    }

    @Override
    public void upgradeGrade(Long userId) {
        Member member = this.getByIdWithTenant(userId);
        UserGrade target = matchGrade(userGradeService.listAllOrdered(), member.getGrowthValue());
        Long targetId = target == null ? null : target.getId();
        if (!Objects.equals(targetId, member.getGradeId())) {
            baseMapper.updateGrade(userId, targetId);
        }
    }

    @Override
    public void setGrade(Long userId, Long gradeId) {
        baseMapper.updateGrade(userId, gradeId);
    }

    /** 等级按 weight 升序传入，取满足 growthValue &lt;= 成员成长值 的最后一个（即最高可达成等级）。 */
    static UserGrade matchGrade(List<UserGrade> gradesAsc, int growthValue) {
        UserGrade target = null;
        for (UserGrade g : gradesAsc) {
            if (growthValue >= g.getGrowthValue()) {
                target = g;
            }
        }
        return target;
    }
}
