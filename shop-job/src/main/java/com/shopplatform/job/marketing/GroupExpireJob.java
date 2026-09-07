package com.shopplatform.job.marketing;

import com.shopplatform.domain.marketing.entity.GroupRecord;
import com.shopplatform.domain.marketing.service.GroupRecordService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Group-buy timeout refund job. See doc3 sec.6 "group timeout not-formed refund, every minute".
 * Per minute, per tenant: scan pending group_record past expire_time, mark fail, and for each
 * linked order: unpaid -> cancel (release stock), paid -> systemRefund (refund + release stock;
 * money refund wired in M5). A single tenant exception does not abort the batch.
 * Spring @Scheduled for now; switch to @XxlJob after XXL-Job integration, domain logic unchanged.
 */
@Component
public class GroupExpireJob {

    private static final Logger log = LoggerFactory.getLogger(GroupExpireJob.class);

    private final ShopService shopService;
    private final GroupRecordService groupRecordService;
    private final OrderService orderService;

    public GroupExpireJob(ShopService shopService,
                           GroupRecordService groupRecordService,
                           OrderService orderService) {
        this.shopService = shopService;
        this.groupRecordService = groupRecordService;
        this.orderService = orderService;
    }

    @Scheduled(cron = "${shop.job.group-expire-cron:0 * * * * ?}")
    public void runEveryMinute() {
        List<Long> shopIds = shopService.listAllShopIds();
        for (Long shopId : shopIds) {
            TenantContext.set(shopId);
            try {
                expireShop();
            } catch (Exception e) {
                log.error("group expire refund failed shopId={}", shopId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void expireShop() {
        List<GroupRecord> records = groupRecordService.listExpiredPending(LocalDateTime.now());
        for (GroupRecord record : records) {
            try {
                groupRecordService.markFail(record.getId());
                for (Order order : orderService.listByGroupRecordId(record.getId())) {
                    if ("unpaid".equals(order.getPayStatus())) {
                        orderService.cancel(order.getId(), "group timeout auto close");
                    } else if ("paid".equals(order.getPayStatus())) {
                        orderService.systemRefund(order.getId(), "group timeout auto refund");
                    }
                }
            } catch (Exception e) {
                log.error("group single refund failed shopId={} recordId={}", TenantContext.getRequired(), record.getId(), e);
            }
        }
    }
}
