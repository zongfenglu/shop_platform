package com.shopplatform.domain.aftersale.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.aftersale.service.AfterSaleTimeoutService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AfterSaleTimeoutServiceImpl implements AfterSaleTimeoutService {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleTimeoutServiceImpl.class);

    private final AfterSaleService afterSaleService;
    private final int applyDays;

    public AfterSaleTimeoutServiceImpl(AfterSaleService afterSaleService,
                                       @Value("${shop.trade.after-sale-apply-days:7}") int applyDays) {
        this.afterSaleService = afterSaleService;
        this.applyDays = applyDays;
    }

    @Override
    public int approveOverdue() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(applyDays);
        List<AfterSale> overdue = TenantContext.ignoreTenant(() -> afterSaleService.list(
                Wrappers.<AfterSale>lambdaQuery()
                        .eq(AfterSale::getStatus, "applying")
                        .lt(AfterSale::getCreateTime, deadline)));
        int changed = 0;
        for (AfterSale row : overdue) {
            try {
                TenantContext.set(row.getShopId());
                afterSaleService.approve(row.getId(), "超时自动同意");
                changed++;
            } catch (Exception e) {
                log.error("售后超时自动同意失败 afterSaleId={} shopId={}", row.getId(), row.getShopId(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return changed;
    }
}
