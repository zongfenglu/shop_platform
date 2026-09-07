package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.domain.platform.entity.SysLog;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.service.ShopExpireRemindService;
import com.shopplatform.domain.shop.service.ShopService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class ShopExpireRemindServiceImpl implements ShopExpireRemindService {

    private static final Set<Long> WINDOWS = Set.of(7L, 3L, 1L);

    private final ShopService shopService;
    private final SysLogService sysLogService;

    public ShopExpireRemindServiceImpl(ShopService shopService, SysLogService sysLogService) {
        this.shopService = shopService;
        this.sysLogService = sysLogService;
    }

    @Override
    public int remindDue() {
        LocalDate today = LocalDate.now();
        List<Shop> shops = shopService.list(Wrappers.<Shop>lambdaQuery()
                .in(Shop::getStatus, ShopStatus.TRIAL.getCode(), ShopStatus.NORMAL.getCode())
                .isNotNull(Shop::getExpireTime));
        int n = 0;
        for (Shop shop : shops) {
            long days = ChronoUnit.DAYS.between(today, shop.getExpireTime().toLocalDate());
            if (!WINDOWS.contains(days)) {
                continue;
            }
            if (alreadyRemindedToday(shop.getId(), today)) {
                continue;
            }
            sysLogService.record(
                    shop.getId(), 0, 0L, "system", false,
                    "shop-expire-remind",
                    "商城「" + shop.getName() + "」将在 " + days + " 天后到期",
                    null);
            n++;
        }
        return n;
    }

    private boolean alreadyRemindedToday(Long shopId, LocalDate today) {
        return sysLogService.count(Wrappers.<SysLog>lambdaQuery()
                .eq(SysLog::getShopId, shopId)
                .eq(SysLog::getAction, "shop-expire-remind")
                .ge(SysLog::getCreateTime, today.atStartOfDay())) > 0;
    }
}
