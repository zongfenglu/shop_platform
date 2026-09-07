package com.shopplatform.job.marketing;

import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 优惠券过期定时任务。对应文档三 §6 营销域日常任务。
 * <p>
 * 把 end_time 已过且仍 unused 的用户券置为 expired，避免前端把过期券当作可用券展示。
 * 全租户循环类任务：按 shop 列表逐个 set/clear 租户上下文，单租户异常不中断整批。
 * <p>
 * 与会员等级升级任务一样，先用 Spring @Scheduled 跑起来，接入 XXL-Job 后换成 @XxlJob，
 * {@link UserCouponService#expireOverdue} 的领域逻辑无需改动。
 */
@Component
public class CouponExpireJob {

    private static final Logger log = LoggerFactory.getLogger(CouponExpireJob.class);

    private final ShopService shopService;
    private final UserCouponService userCouponService;

    public CouponExpireJob(ShopService shopService, UserCouponService userCouponService) {
        this.shopService = shopService;
        this.userCouponService = userCouponService;
    }

    /** 每天凌晨 00:10 执行一次，早于会员等级升级任务。 */
    @Scheduled(cron = "${shop.job.coupon-expire-cron:0 10 0 * * ?}")
    public void runDaily() {
        List<Long> shopIds = shopService.listAllShopIds();
        log.info("优惠券过期任务开始 shopCount={}", shopIds.size());
        int totalExpired = 0;
        for (Long shopId : shopIds) {
            TenantContext.set(shopId);
            try {
                totalExpired += userCouponService.expireOverdue();
            } catch (Exception e) {
                log.error("优惠券过期失败 shopId={}", shopId, e);
            } finally {
                TenantContext.clear();
            }
        }
        log.info("优惠券过期任务结束 shopCount={} totalExpired={}", shopIds.size(), totalExpired);
    }
}
