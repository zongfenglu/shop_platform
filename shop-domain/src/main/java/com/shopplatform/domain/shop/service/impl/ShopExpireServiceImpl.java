package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.service.ShopExpireService;
import com.shopplatform.domain.shop.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShopExpireServiceImpl implements ShopExpireService {

    private static final Logger log = LoggerFactory.getLogger(ShopExpireServiceImpl.class);

    private final ShopService shopService;

    public ShopExpireServiceImpl(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public int expireDue() {
        LocalDateTime now = LocalDateTime.now();
        List<Shop> due = shopService.list(Wrappers.<Shop>lambdaQuery()
                .in(Shop::getStatus, ShopStatus.TRIAL.getCode(), ShopStatus.NORMAL.getCode())
                .isNotNull(Shop::getExpireTime)
                .lt(Shop::getExpireTime, now));
        int changed = 0;
        for (Shop shop : due) {
            try {
                shop.setStatus(ShopStatus.EXPIRED.getCode());
                shopService.updateById(shop);
                changed++;
            } catch (Exception e) {
                log.error("租户到期流转失败 shopId={}", shop.getId(), e);
            }
        }
        return changed;
    }
}
