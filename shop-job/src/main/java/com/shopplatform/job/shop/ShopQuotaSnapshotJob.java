package com.shopplatform.job.shop;

import com.shopplatform.domain.shop.service.ShopQuotaSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 配额用量统计。对应文档三 §6「每天 01:00 写 shop_quota_usage」。
 * 领域循环在 {@link ShopQuotaSnapshotService}，接入 XXL-Job 后只换注解。
 */
@Component
public class ShopQuotaSnapshotJob {

    private static final Logger log = LoggerFactory.getLogger(ShopQuotaSnapshotJob.class);

    private final ShopQuotaSnapshotService shopQuotaSnapshotService;

    public ShopQuotaSnapshotJob(ShopQuotaSnapshotService shopQuotaSnapshotService) {
        this.shopQuotaSnapshotService = shopQuotaSnapshotService;
    }

    @Scheduled(cron = "${shop.job.quota-snapshot-cron:0 0 1 * * ?}")
    public void runDaily() {
        log.info("配额用量统计开始");
        int ok = shopQuotaSnapshotService.snapshotAll();
        log.info("配额用量统计结束 shops={}", ok);
    }
}
