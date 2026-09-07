package com.shopplatform.job.order;

import com.shopplatform.domain.order.service.OrderTimeoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 未支付超时关单兜底。主链路是延时消息，本任务每 5 分钟扫漏单。 */
@Component
public class OrderTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutJob.class);

    private final OrderTimeoutService orderTimeoutService;

    public OrderTimeoutJob(OrderTimeoutService orderTimeoutService) {
        this.orderTimeoutService = orderTimeoutService;
    }

    @Scheduled(cron = "${shop.job.order-timeout-cron:0 */5 * * * ?}")
    public void runEveryFiveMinutes() {
        int n = orderTimeoutService.closeOverdue();
        if (n > 0) {
            log.info("超时关单兜底 closed={}", n);
        }
    }
}
