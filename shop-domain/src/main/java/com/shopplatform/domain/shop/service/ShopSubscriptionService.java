package com.shopplatform.domain.shop.service;

import com.shopplatform.domain.shop.entity.PackageTpl;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.entity.ShopPackage;

import java.time.LocalDateTime;
import java.util.List;

/** 商户套餐概览、续费/升级下单，以及平台确认到账后生效。在线支付按排期后续接入，本链路走线下转账。 */
public interface ShopSubscriptionService {

    Overview overview(Long shopId);

    ShopOrder placeOrder(Long shopId, String type, Long packageTplId, int durationMonth);

    ShopOrder confirmPaid(Long shopOrderId);

    record QuotaUsage(String key, String label, long used, long max, int percent) {
    }

    record Overview(
            String shopName,
            String shopStatus,
            LocalDateTime expireTime,
            long daysLeft,
            ShopPackage currentPackage,
            List<QuotaUsage> quotas,
            List<PackageTpl> plans,
            List<ShopOrder> orders
    ) {
    }
}
