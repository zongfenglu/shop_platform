package com.shopplatform.domain.dealer.service;

import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface DealerSettingService extends TenantSafeService<DealerSetting> {

    /** 获取或创建当前租户的分销设置（默认禁用，10%佣金）。 */
    DealerSetting getOrCreate();

    /** 保存分销设置（覆盖式 upsert）。 */
    void saveSetting(DealerSetting setting);
}
