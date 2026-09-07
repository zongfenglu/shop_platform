package com.shopplatform.domain.member.service;

import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.math.BigDecimal;

public interface MemberService extends TenantSafeService<Member> {

    /**
     * 手机号免密登录：查到即返回，查不到即注册（见文档三 §8——短信验证码网关接入前的开发期简化方案，
     * 正式短信校验在 Sprint 7 会员体系接入短信网关时补齐）。调用时 TenantContext 必须已由
     * ClientTenantFilter 设好（消费者端先识别商城再识别用户），本方法内部按当前租户直接查库/建号。
     */
    Member loginOrRegister(String mobile);

    /**
     * 原子调整余额。delta 带正负号：充值+/消费-/退款+/后台调整±/佣金+。
     * 扣减后余额不可透支，否则抛业务异常。变动成功后写一条 user_balance_log，
     * before/after 按更新后的余额回算，保证与 user.balance 一致。
     * <p>
     * scene 取值见文档三 §3.4：recharge/consume/refund/admin/commission。
     */
    void adjustBalance(Long userId, BigDecimal delta, String scene, String remark, Long orderId);

    /** 原子调整积分，扣减后不可透支，写 user_points_log。scene: recharge/consume/refund/admin/sign/order。 */
    void adjustPoints(Long userId, int delta, String scene, String remark);

    /** 累加成长值（不写流水，成长值变动由消费/充值间接驱动）。 */
    void addGrowth(Long userId, int delta);

    /**
     * 订单支付成功后累计消费统计 + 成长值（1 元 = 1 成长值，截断小数）+ 触发等级重算。
     * 幂等性由调用方保证（PaymentServiceImpl 仅在 markPaid 首次成功时调用一次）。
     */
    void recordPayment(Long userId, BigDecimal payMoney);

    /** 按当前成长值重算并更新会员等级（取满足 growth_value &lt;= 成长值 中 weight 最大的等级）。 */
    void upgradeGrade(Long userId);

    /** 直接设置会员等级（供批量升级任务使用，不再重复读等级表）。 */
    void setGrade(Long userId, Long gradeId);
}
