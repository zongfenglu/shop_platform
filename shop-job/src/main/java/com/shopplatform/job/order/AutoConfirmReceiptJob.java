package com.shopplatform.job.order;

import com.shopplatform.domain.order.service.OrderAutoConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 快递发货超时自动确认收货。默认每小时，窗口 15 天。
 */
@Component
public class AutoConfirmReceiptJob {

    private static final Logger log = LoggerFactory.getLogger(AutoConfirmReceiptJob.class);

    private final OrderAutoConfirmService orderAutoConfirmService;

    public AutoConfirmReceiptJob(OrderAutoConfirmService orderAutoConfirmService) {
        this.orderAutoConfirmService = orderAutoConfirmService;
    }

    @Scheduled(cron = "${shop.job.auto-confirm-cron:0 0 * * * ?}")
    public void runHourly() {
        log.info("自动确认收货开始");
        int changed = orderAutoConfirmService.confirmOverdue();
        log.info("自动确认收货结束 confirmed={}", changed);
    }
}
