package com.shopplatform.domain.setting.service;

import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 店铺地图 Key，没有则回落到平台默认的高德 JS Key。 */
@Component
public class MapKeyResolver {

    private final StoreOperationSettingService settingService;
    private final String platformAmapJsKey;

    public MapKeyResolver(StoreOperationSettingService settingService,
                          @Value("${shop.map.amap-js-key:}") String platformAmapJsKey) {
        this.settingService = settingService;
        this.platformAmapJsKey = platformAmapJsKey == null ? "" : platformAmapJsKey.trim();
    }

    public String provider() {
        StoreOperationSetting setting = settingService.getOrCreate();
        if (setting != null && StringUtils.hasText(setting.getMapProvider())) {
            return setting.getMapProvider().trim();
        }
        return "amap";
    }

    public String jsKey() {
        StoreOperationSetting setting = settingService.getOrCreate();
        if (setting != null && StringUtils.hasText(setting.getMapApiKey())) {
            return setting.getMapApiKey().trim();
        }
        return platformAmapJsKey;
    }
}
