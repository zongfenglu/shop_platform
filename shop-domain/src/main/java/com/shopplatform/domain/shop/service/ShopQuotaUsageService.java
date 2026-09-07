package com.shopplatform.domain.shop.service;

import com.shopplatform.domain.shop.entity.ShopQuotaUsage;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface ShopQuotaUsageService extends TenantSafeService<ShopQuotaUsage> {

    List<ShopQuotaUsage> listLatestSnapshots(int limit);
}
