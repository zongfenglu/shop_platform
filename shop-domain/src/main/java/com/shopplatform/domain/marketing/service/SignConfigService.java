package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.SignConfig;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface SignConfigService extends TenantSafeService<SignConfig> {

    /** 获取或创建当前租户的签到配置（默认 dailyPoints=2，无连续奖励规则）。 */
    SignConfig getOrCreate();

    /** 保存签到配置（覆盖式 upsert）。 */
    void saveConfig(int dailyPoints, List<ContinuousRule> rules);
}
