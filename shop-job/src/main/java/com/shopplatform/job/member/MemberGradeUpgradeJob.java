package com.shopplatform.job.member;

import com.shopplatform.domain.member.service.MemberGradeUpgradeService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会员等级自动升级定时任务。对应文档三 §6："会员等级升级 每天 按成长值"。
 * <p>
 * 全租户循环类任务（文档三 §6 注释）：按 shop 列表逐个 set/clear 租户上下文，
 * 单租户异常不得中断整批——所以每个租户的处理包在 try/catch 里，异常只记日志。
 * <p>
 * 文档清单里此任务挂在 XXL-Job 下；shop-job 当前尚未接入 XXL-Job 执行器（见 pom 注释），
 * 先用 Spring @Scheduled 让逻辑跑起来，等 XXL-Job 接入后把 @Scheduled 换成 @XxlJob 即可，
 * {@link MemberGradeUpgradeService#upgradeAllForCurrentTenant} 的领域逻辑无需改动。
 */
@Component
public class MemberGradeUpgradeJob {

    private static final Logger log = LoggerFactory.getLogger(MemberGradeUpgradeJob.class);

    private final ShopService shopService;
    private final MemberGradeUpgradeService memberGradeUpgradeService;

    public MemberGradeUpgradeJob(ShopService shopService, MemberGradeUpgradeService memberGradeUpgradeService) {
        this.shopService = shopService;
        this.memberGradeUpgradeService = memberGradeUpgradeService;
    }

    /** 每天凌晨 00:30 执行一次。 */
    @Scheduled(cron = "${shop.job.member-grade-upgrade-cron:0 30 0 * * ?}")
    public void runDaily() {
        List<Long> shopIds = shopService.listAllShopIds();
        log.info("会员等级升级任务开始 shopCount={}", shopIds.size());
        int totalChanged = 0;
        for (Long shopId : shopIds) {
            TenantContext.set(shopId);
            try {
                totalChanged += memberGradeUpgradeService.upgradeAllForCurrentTenant();
            } catch (Exception e) {
                // 单租户异常不中断整批，只记日志——见类注释
                log.error("会员等级升级失败 shopId={}", shopId, e);
            } finally {
                TenantContext.clear();
            }
        }
        log.info("会员等级升级任务结束 shopCount={} totalChanged={}", shopIds.size(), totalChanged);
    }
}
