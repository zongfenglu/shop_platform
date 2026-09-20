package com.shopplatform.clientapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import com.shopplatform.domain.setting.service.StoreOperationSettingService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 分享页配置。未设置文案或图片时由前端使用内置默认海报。 */
@RestController
@RequestMapping("/api/share")
public class ConsumerShareController {

    private final StoreOperationSettingService settingService;

    public ConsumerShareController(StoreOperationSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        StoreOperationSetting setting = settingService.getOrCreate();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", textOr(setting.getShareTitle(), "分享好物"));
        result.put("subtitle", textOr(setting.getShareSubtitle(), "把商品、活动分享给朋友，一起享受优惠"));
        result.put("brand", textOr(setting.getShareBrand(), "商城精选"));
        result.put("imageUrl", setting.getShareImageUrl());
        return Result.ok(result);
    }

    private static String textOr(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
