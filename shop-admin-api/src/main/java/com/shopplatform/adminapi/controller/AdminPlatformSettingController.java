package com.shopplatform.adminapi.controller;

import com.shopplatform.adminapi.dto.PlatformSettingResponse;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.platform.service.PlatformSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/settings")
public class AdminPlatformSettingController {

    private static final List<String> SETTING_KEYS = List.of("site", "storage", "sms", "express", "wechat", "pay");

    private final PlatformSettingService platformSettingService;

    public AdminPlatformSettingController(PlatformSettingService platformSettingService) {
        this.platformSettingService = platformSettingService;
    }

    @GetMapping
    public Result<PlatformSettingResponse> getAll() {
        Map<String, Map<String, Object>> stored = platformSettingService.listValues(SETTING_KEYS);
        return Result.ok(new PlatformSettingResponse(
                merge("site", stored),
                merge("storage", stored),
                merge("sms", stored),
                merge("express", stored),
                merge("wechat", stored),
                merge("pay", stored)
        ));
    }

    @PutMapping("/{key}")
    public Result<Void> save(@PathVariable String key, @RequestBody Map<String, Object> value) {
        ensureSupportedKey(key);
        Map<String, Object> toSave = new LinkedHashMap<>(value == null ? Map.of() : value);
        if ("wechat".equals(key)) {
            Map<String, Object> previous = platformSettingService.findValue("wechat");
            keepIfMasked(toSave, previous, "componentSecret");
            keepIfMasked(toSave, previous, "token");
            keepIfMasked(toSave, previous, "encodingAesKey");
        }
        platformSettingService.saveValue(key, toSave);
        return Result.ok();
    }

    private Map<String, Object> merge(String key, Map<String, Map<String, Object>> stored) {
        Map<String, Object> result = new LinkedHashMap<>(defaults(key));
        result.putAll(stored.getOrDefault(key, Map.of()));
        if ("wechat".equals(key)) {
            mask(result, "componentSecret");
            mask(result, "token");
            mask(result, "encodingAesKey");
        }
        return result;
    }

    private Map<String, Object> defaults(String key) {
        return switch (key) {
            case "site" -> mapOf(
                    "platformName", "多开云商城",
                    "customerPhone", "",
                    "icpNo", "",
                    "contactEmail", ""
            );
            case "storage" -> mapOf(
                    "provider", "local",
                    "bucket", "",
                    "region", ""
            );
            case "sms" -> mapOf(
                    "provider", "",
                    "accessKey", "",
                    "accessSecret", "",
                    "signName", ""
            );
            case "express" -> mapOf(
                    "provider", "",
                    "apiKey", ""
            );
            case "wechat" -> mapOf(
                    "componentAppId", "",
                    "componentSecret", "",
                    "token", "",
                    "encodingAesKey", "",
                    "callbackUrl", "http://localhost:8085/notify/wechat/component"
            );
            case "pay" -> mapOf(
                    "channel", "",
                    "mchId", "",
                    "certSerialNo", ""
            );
            default -> Map.of();
        };
    }

    private void ensureSupportedKey(String key) {
        if (!SETTING_KEYS.contains(key)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的配置项");
        }
    }

    private static void mask(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v != null && !String.valueOf(v).isBlank()) {
            map.put(key, "********");
            map.put(key + "Set", true);
        } else {
            map.put(key + "Set", false);
        }
    }

    private static void keepIfMasked(Map<String, Object> incoming, Map<String, Object> previous, String key) {
        Object v = incoming.get(key);
        if (v == null || "********".equals(String.valueOf(v))) {
            incoming.put(key, previous.getOrDefault(key, ""));
        }
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
