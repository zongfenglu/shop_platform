package com.shopplatform.job.aftersale;

import com.shopplatform.domain.aftersale.service.AfterSaleTimeoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 售后申请超时自动同意。默认每小时，窗口 7 天。不触发微信退款。
 */
@Component
public class AfterSaleTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleTimeoutJob.class);

    private final AfterSaleTimeoutService afterSaleTimeoutService;

    public AfterSaleTimeoutJob(AfterSaleTimeoutService afterSaleTimeoutService) {
        this.afterSaleTimeoutService = afterSaleTimeoutService;
    }

    @Scheduled(cron = "${shop.job.after-sale-timeout-cron:0 10 * * * ?}")
    public void runHourly() {
        log.info("售后超时自动同意开始");
        int changed = afterSaleTimeoutService.approveOverdue();
        log.info("售后超时自动同意结束 approved={}", changed);
    }
}
