package com.shopplatform.job.marketing;

import com.shopplatform.domain.marketing.entity.BargainRecord;
import com.shopplatform.domain.marketing.service.BargainRecordService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bargain expire job. See doc3 sec.6 "bargain activity expire, every hour".
 * Per hour, per tenant: scan ongoing/done bargain_record past expire_time, mark expired
 * (the record can no longer be used to place an order). A single tenant exception does not
 * abort the batch. Spring @Scheduled for now; switch to @XxlJob later, domain logic unchanged.
 */
@Component
public class BargainExpireJob {

    private static final Logger log = LoggerFactory.getLogger(BargainExpireJob.class);

    private final ShopService shopService;
    private final BargainRecordService bargainRecordService;

    public BargainExpireJob(ShopService shopService, BargainRecordService bargainRecordService) {
        this.shopService = shopService;
        this.bargainRecordService = bargainRecordService;
    }

    @Scheduled(cron = "${shop.job.bargain-expire-cron:0 0 * * * ?}")
    public void runEveryHour() {
        List<Long> shopIds = shopService.listAllShopIds();
        for (Long shopId : shopIds) {
            TenantContext.set(shopId);
            try {
                expireShop();
            } catch (Exception e) {
                log.error("bargain expire failed shopId={}", shopId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void expireShop() {
        List<BargainRecord> records = bargainRecordService.listExpired(LocalDateTime.now());
        for (BargainRecord record : records) {
            try {
                bargainRecordService.markExpired(record.getId());
            } catch (Exception e) {
                log.error("bargain single expire failed shopId={} recordId={}", TenantContext.getRequired(), record.getId(), e);
            }
        }
    }
}
