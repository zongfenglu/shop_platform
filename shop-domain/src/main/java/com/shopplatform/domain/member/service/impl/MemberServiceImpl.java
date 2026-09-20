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
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    /** 用户合并时需要迁移的直接 user_id 关联表。 */
    private static final List<String> USER_REFERENCE_TABLES = List.of(
            "cart", "user_address", "user_balance_log", "user_points_log", "recharge_order",
            "user_coupon", "after_sale", "goods_comment", "sign_record", "exchange_record",
            "`order`", "bargain_record", "dealer_withdraw"
    );

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
        initializeMember(member);
        this.save(member);
        return member;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member loginOrRegisterWechat(String openId, String unionId) {
        if (!StringUtils.hasText(openId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "微信 openId 不能为空");
        }
        Member existing = this.getOne(Wrappers.<Member>lambdaQuery().eq(Member::getOpenId, openId.trim()));
        if (existing != null) {
            baseMapper.updateLastLoginTime(existing.getId(), LocalDateTime.now());
            return existing;
        }
        Member member = new Member();
        member.setNickname("微信用户");
        member.setPlatform("mp");
        member.setOpenId(openId.trim());
        member.setUnionId(StringUtils.hasText(unionId) ? unionId.trim() : null);
        initializeMember(member);
        this.save(member);
        return member;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member bindMobile(Long userId, String mobile) {
        if (!StringUtils.hasText(mobile) || !mobile.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "手机号格式不正确");
        }
        Member member = this.getByIdWithTenant(userId);
        Member occupied = this.getOne(Wrappers.<Member>lambdaQuery().eq(Member::getMobile, mobile));
        if (occupied != null && !occupied.getId().equals(userId)) {
            return mergeWechatAccount(member, occupied, mobile);
        }
        member.setMobile(mobile);
        this.updateById(member);
        return member;
    }

    /**
     * 微信账号授权手机号已存在时，以手机号账号为主账号完成合并。
     * 这样 H5 端已有的订单、积分、余额、地址等历史数据不会被新建的微信账号割裂。
     */
    private Member mergeWechatAccount(Member wechatMember, Member mobileMember, String mobile) {
        if (StringUtils.hasText(mobileMember.getOpenId())
                && StringUtils.hasText(wechatMember.getOpenId())
                && !Objects.equals(mobileMember.getOpenId(), wechatMember.getOpenId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该手机号已绑定其他微信账号");
        }

        Long sourceId = wechatMember.getId();
        Long targetId = mobileMember.getId();
        Long shopId = TenantContext.getRequired();
        // 先清理唯一索引冲突，主账号已有的签到/砍价记录优先保留。
        getBaseMapper().deleteDuplicateSignRecords(sourceId, targetId, shopId);
        getBaseMapper().deleteDuplicateBargainRecords(sourceId, targetId, shopId);
        for (String table : USER_REFERENCE_TABLES) {
            getBaseMapper().moveUserReference(table, "user_id", sourceId, targetId, shopId);
        }
        // 拼团团长字段不是 user_id，订单与营销记录仍必须归并到主账号。
        getBaseMapper().moveUserReference("group_record", "leader_user_id", sourceId, targetId, shopId);
        getBaseMapper().mergeDealerTotals(sourceId, targetId, shopId);
        getBaseMapper().moveDealerOrders(sourceId, targetId, shopId);
        getBaseMapper().moveDealerWithdraws(sourceId, targetId, shopId);
        getBaseMapper().deleteMergedDealerUser(sourceId, targetId, shopId);
        getBaseMapper().moveDealerUserWhenTargetMissing(sourceId, targetId, shopId);

        mobileMember.setMobile(mobile);
        if (!StringUtils.hasText(mobileMember.getOpenId())) {
            // 目标账号写入同一个 open_id 前，先释放源账号的唯一索引。
            getBaseMapper().clearOpenId(sourceId, shopId);
            mobileMember.setOpenId(wechatMember.getOpenId());
            mobileMember.setUnionId(wechatMember.getUnionId());
            mobileMember.setPlatform("mp");
        }
        if (!StringUtils.hasText(mobileMember.getAvatar()) && StringUtils.hasText(wechatMember.getAvatar())) {
            mobileMember.setAvatar(wechatMember.getAvatar());
        }
        if (!StringUtils.hasText(mobileMember.getNickname())
                || mobileMember.getNickname().startsWith("用户")) {
            mobileMember.setNickname(wechatMember.getNickname());
        }
        mobileMember.setBalance(safeAmount(mobileMember.getBalance()).add(safeAmount(wechatMember.getBalance())));
        mobileMember.setPoints(safeInt(mobileMember.getPoints()) + safeInt(wechatMember.getPoints()));
        mobileMember.setGrowthValue(safeInt(mobileMember.getGrowthValue()) + safeInt(wechatMember.getGrowthValue()));
        mobileMember.setPayMoney(safeAmount(mobileMember.getPayMoney()).add(safeAmount(wechatMember.getPayMoney())));
        mobileMember.setPayCount(safeInt(mobileMember.getPayCount()) + safeInt(wechatMember.getPayCount()));
        mobileMember.setIsBlack(Math.max(safeInt(mobileMember.getIsBlack()), safeInt(wechatMember.getIsBlack())));
        mobileMember.setLastLoginTime(LocalDateTime.now());
        this.updateById(mobileMember);
        this.removeById(sourceId);
        return mobileMember;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member updateProfile(Long userId, String nickname) {
        String value = nickname == null ? "" : nickname.trim();
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请输入昵称");
        }
        if (value.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "昵称不能超过32个字符");
        }
        Member member = this.getByIdWithTenant(userId);
        member.setNickname(value);
        this.updateById(member);
        return member;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member updateAvatar(Long userId, String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl) || avatarUrl.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "头像地址不正确");
        }
        Member member = this.getByIdWithTenant(userId);
        member.setAvatar(avatarUrl.trim());
        this.updateById(member);
        return member;
    }

    private void initializeMember(Member member) {
        member.setStatus(1);
        member.setBalance(BigDecimal.ZERO);
        member.setPoints(0);
        member.setGrowthValue(0);
        member.setPayMoney(BigDecimal.ZERO);
        member.setPayCount(0);
        member.setIsBlack(0);
        member.setLastLoginTime(LocalDateTime.now());
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
