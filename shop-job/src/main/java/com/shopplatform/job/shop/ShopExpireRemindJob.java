package com.shopplatform.job.shop;

import com.shopplatform.domain.shop.service.ShopExpireRemindService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 租户到期前提醒。无短信通道，写入 sys_log。 */
@Component
public class ShopExpireRemindJob {

    private static final Logger log = LoggerFactory.getLogger(ShopExpireRemindJob.class);

    private final ShopExpireRemindService shopExpireRemindService;

    public ShopExpireRemindJob(ShopExpireRemindService shopExpireRemindService) {
        this.shopExpireRemindService = shopExpireRemindService;
    }

    @Scheduled(cron = "${shop.job.shop-expire-remind-cron:0 0 9 * * ?}")
    public void runDaily() {
        log.info("租户到期提醒开始");
        int n = shopExpireRemindService.remindDue();
        log.info("租户到期提醒结束 reminded={}", n);
    }
}
