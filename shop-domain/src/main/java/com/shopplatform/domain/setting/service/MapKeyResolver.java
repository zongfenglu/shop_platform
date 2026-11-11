package com.shopplatform.domain.setting.service;

import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 店铺地图 Key：Web 端 JS Key + 安全密钥用于 H5/商户后台，小程序 Key 单独下发。 */
@Component
public class MapKeyResolver {

    private final StoreOperationSettingService settingService;
    private final String platformAmapJsKey;
    private final String platformAmapJsSecret;
    private final String platformAmapMpKey;

    public MapKeyResolver(StoreOperationSettingService settingService,
                          @Value("${shop.map.amap-js-key:}") String platformAmapJsKey,
                          @Value("${shop.map.amap-js-secret:}") String platformAmapJsSecret,
                          @Value("${shop.map.amap-mp-key:}") String platformAmapMpKey) {
        this.settingService = settingService;
        this.platformAmapJsKey = trim(platformAmapJsKey);
        this.platformAmapJsSecret = trim(platformAmapJsSecret);
        this.platformAmapMpKey = trim(platformAmapMpKey);
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
            String shopKey = setting.getMapApiKey().trim();
            // 微信小程序 Key 不能用于 JS API，回落到平台 Web 端 Key
            if (!shopKey.equals(platformAmapMpKey)) {
                return shopKey;
            }
        }
        return platformAmapJsKey;
    }

    public String securityJsCode() {
        return platformAmapJsSecret;
    }

    public String mpKey() {
        return platformAmapMpKey;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
