package com.shopplatform.domain.member.service;

/**
 * 会员等级自动升级。对应文档三 §6 定时任务"会员等级升级 每天 按成长值"。
 * 逻辑抽到领域层（可单测），由 shop-job 的定时任务按租户循环调用。
 */
public interface MemberGradeUpgradeService {

    /**
     * 重算当前租户全部会员的等级。调用前 TenantContext 必须已 set 为目标租户，
     * 调用方负责在 finally 里 clear。返回发生变更的会员数。
     */
    int upgradeAllForCurrentTenant();
}
