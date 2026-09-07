package com.shopplatform.job.marketing;

import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import com.shopplatform.domain.marketing.service.SeckillStockService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 秒杀库存对账 + 活动状态流转定时任务。对应文档三 §6"秒杀/拼团活动状态流转 每分钟"。
 * <p>
 * 每分钟按租户循环：
 * - 进行中的秒杀（time_ids 非空、在日期范围内、status=on）：用 DB 已售修正 Redis 库存 = seckill_num - sold，自愈任何漂移；
 * - 已结束的秒杀：清理 Redis stock key（bought key 带 TTL，自然过期）。
 * 限时折扣（time_ids 空）无 Redis 限量池，跳过。
 * 先用 Spring @Scheduled，接入 XXL-Job 后换 @XxlJob，领域逻辑不变。
 */
@Component
public class SeckillStockJob {

    private static final Logger log = LoggerFactory.getLogger(SeckillStockJob.class);

    private final ShopService shopService;
    private final SeckillActiveService seckillActiveService;
    private final SeckillGoodsService seckillGoodsService;
    private final SeckillStockService seckillStockService;

    public SeckillStockJob(ShopService shopService,
                            SeckillActiveService seckillActiveService,
                            SeckillGoodsService seckillGoodsService,
                            SeckillStockService seckillStockService) {
        this.shopService = shopService;
        this.seckillActiveService = seckillActiveService;
        this.seckillGoodsService = seckillGoodsService;
        this.seckillStockService = seckillStockService;
    }

    @Scheduled(cron = "${shop.job.seckill-stock-cron:0 * * * * ?}")
    public void runEveryMinute() {
        List<Long> shopIds = shopService.listAllShopIds();
        for (Long shopId : shopIds) {
            TenantContext.set(shopId);
            try {
                reconcileShop();
            } catch (Exception e) {
                log.error("秒杀库存对账失败 shopId={}", shopId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void reconcileShop() {
        LocalDate today = LocalDate.now();
        for (SeckillActive active : seckillActiveService.listAll()) {
            if (!isSeckill(active.getTimeIds())) {
                continue; // 限时折扣无 Redis 限量池
            }
            boolean inRange = "on".equals(active.getStatus())
                    && !today.isBefore(active.getStartDate()) && !today.isAfter(active.getEndDate());
            for (SeckillGoods sg : seckillGoodsService.listByActive(active.getId())) {
                if (inRange) {
                    int sold = sg.getSold() == null ? 0 : sg.getSold();
                    int num = sg.getSeckillNum() == null ? 0 : sg.getSeckillNum();
                    seckillStockService.reconcile(active.getId(), sg.getSkuId(), num, sold);
                } else {
                    seckillStockService.evictStock(active.getId(), sg.getSkuId());
                }
            }
        }
    }

    private boolean isSeckill(String timeIdsJson) {
        return StringUtils.hasText(timeIdsJson) && !timeIdsJson.equals("[]");
    }
}
