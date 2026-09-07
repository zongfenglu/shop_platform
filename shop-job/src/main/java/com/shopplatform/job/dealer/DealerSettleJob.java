package com.shopplatform.job.dealer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopplatform.domain.dealer.entity.DealerOrder;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.mapper.DealerOrderMapper;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import com.shopplatform.domain.dealer.service.DealerUserService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 佣金结算定时任务。每30分钟扫描 pending 状态的 dealer_order，
 * 确认对应订单已收货且超过售后期（简化：7天）后转为 settled，累加可提现佣金。
 * 后续可迁移至 XXL-Job。
 */
@Component
public class DealerSettleJob {

    private static final Logger log = LoggerFactory.getLogger(DealerSettleJob.class);

    private final ShopService shopService;
    private final DealerOrderService dealerOrderService;
    private final DealerUserService dealerUserService;
    private final DealerOrderMapper dealerOrderMapper;

    public DealerSettleJob(ShopService shopService,
                            DealerOrderService dealerOrderService,
                            DealerUserService dealerUserService,
                            DealerOrderMapper dealerOrderMapper) {
        this.shopService = shopService;
        this.dealerOrderService = dealerOrderService;
        this.dealerUserService = dealerUserService;
        this.dealerOrderMapper = dealerOrderMapper;
    }

    @Scheduled(cron = "${shop.job.dealer-settle-cron:0 */30 * * * ?}")
    public void settle() {
        List<Long> shopIds = shopService.listAllShopIds();
        for (Long shopId : shopIds) {
            try {
                TenantContext.set(shopId);
                settlePerShop();
            } catch (Exception e) {
                log.error("佣金结算异常 shopId={}", shopId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void settlePerShop() {
        // 查所有 pending 佣金记录
        List<DealerOrder> pendingOrders = dealerOrderService.list(
                new LambdaQueryWrapper<DealerOrder>().eq(DealerOrder::getStatus, "pending"));

        int settled = 0;
        for (DealerOrder dOrder : pendingOrders) {
            try {
                // 简化：7天前创建的 pending 记录自动转为 settled
                // 完整实现需要查 order 表的 confirm_receipt_time + after_sale 表判断售后期
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                if (dOrder.getCreateTime() != null && dOrder.getCreateTime().isBefore(sevenDaysAgo)) {
                    dOrder.setStatus("settled");
                    dOrder.setSettleTime(LocalDateTime.now());
                    dealerOrderService.updateById(dOrder);

                    // 累加可提现佣金
                    DealerUser du = dealerUserService.getByIdWithTenant(dOrder.getDealerUserId());
                    if (du != null) {
                        du.setAvailableCommission(du.getAvailableCommission().add(dOrder.getCommissionAmount()));
                        du.setFrozenCommission(du.getFrozenCommission().subtract(dOrder.getCommissionAmount()));
                        dealerUserService.updateById(du);
                    }
                    settled++;
                }
            } catch (Exception e) {
                log.error("结算单条佣金失败 dealerOrderId={}", dOrder.getId(), e);
            }
        }
        if (settled > 0) {
            log.info("佣金结算完成 shopId={} settled={}", TenantContext.get(), settled);
        }
    }
}
