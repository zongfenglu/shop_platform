package com.shopplatform.domain.platform.service;

import com.shopplatform.domain.platform.entity.PlatformSetting;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.Collection;
import java.util.Map;

public interface PlatformSettingService extends TenantSafeService<PlatformSetting> {

    Map<String, Map<String, Object>> listValues(Collection<String> keys);

    Map<String, Object> findValue(String settingKey);

    PlatformSetting saveValue(String settingKey, Map<String, Object> value);
}
