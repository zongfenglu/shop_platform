package com.shopplatform.job.mp;

import com.shopplatform.domain.mp.MpComponentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 刷新第三方平台 component_access_token。票据本身由微信推送到 shop-mp，
 * 本任务每 10 分钟用最新 ticket 换 token 并写入 Redis。
 */
@Component
public class WechatTicketJob {

    private static final Logger log = LoggerFactory.getLogger(WechatTicketJob.class);

    private final MpComponentService mpComponentService;

    public WechatTicketJob(MpComponentService mpComponentService) {
        this.mpComponentService = mpComponentService;
    }

    @Scheduled(cron = "${shop.job.wechat-ticket-cron:0 */10 * * * ?}")
    public void run() {
        try {
            var token = mpComponentService.refreshComponentToken();
            log.info("微信 component_access_token 已刷新 expiresIn={}", token.expiresIn());
        } catch (Exception e) {
            log.warn("微信票据刷新跳过: {}", e.getMessage());
        }
    }
}
