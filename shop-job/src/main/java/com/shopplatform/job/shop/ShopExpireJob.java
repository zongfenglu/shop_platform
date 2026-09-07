package com.shopplatform.job.shop;

import com.shopplatform.domain.shop.service.ShopExpireService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 租户到期检查。对应文档三 §6「每天 00:05 状态流转」。
 */
@Component
public class ShopExpireJob {

    private static final Logger log = LoggerFactory.getLogger(ShopExpireJob.class);

    private final ShopExpireService shopExpireService;

    public ShopExpireJob(ShopExpireService shopExpireService) {
        this.shopExpireService = shopExpireService;
    }

    @Scheduled(cron = "${shop.job.shop-expire-cron:0 5 0 * * ?}")
    public void runDaily() {
        log.info("租户到期检查开始");
        int changed = shopExpireService.expireDue();
        log.info("租户到期检查结束 expired={}", changed);
    }
}
