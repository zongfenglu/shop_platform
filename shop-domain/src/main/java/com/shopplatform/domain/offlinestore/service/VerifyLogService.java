package com.shopplatform.domain.offlinestore.service;

import com.shopplatform.domain.offlinestore.entity.VerifyLog;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface VerifyLogService extends TenantSafeService<VerifyLog> {

    /** 核销记录列表；storeId 为 null 表示不按门店过滤（店主/all数据权限），非空表示仅本店（店员数据权限）。 */
    List<VerifyLog> listByStore(Long storeId);
}
