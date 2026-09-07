package com.shopplatform.domain.offlinestore.service;

import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface OfflineStoreService extends TenantSafeService<OfflineStore> {

    /** 消费者端可选的自提门店（仅 enabled）。 */
    List<OfflineStore> listEnabled();
}
