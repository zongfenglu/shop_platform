package com.shopplatform.domain.setting.service;

import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface StoreOperationSettingService extends TenantSafeService<StoreOperationSetting> {
    StoreOperationSetting getOrCreate();
}
