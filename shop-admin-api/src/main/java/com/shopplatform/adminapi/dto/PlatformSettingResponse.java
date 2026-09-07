package com.shopplatform.adminapi.dto;

import java.util.Map;

public record PlatformSettingResponse(
        Map<String, Object> site,
        Map<String, Object> storage,
        Map<String, Object> sms,
        Map<String, Object> express,
        Map<String, Object> wechat,
        Map<String, Object> pay
) {
}
