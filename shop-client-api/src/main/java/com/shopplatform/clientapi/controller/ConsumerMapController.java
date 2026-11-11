package com.shopplatform.clientapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.setting.service.MapKeyResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 消费者端地图配置。高德 JS Key 本身按域名白名单使用，可下发给 H5 选点/导航。 */
@RestController
@RequestMapping("/api/map")
public class ConsumerMapController {

    private final MapKeyResolver mapKeyResolver;

    public ConsumerMapController(MapKeyResolver mapKeyResolver) {
        this.mapKeyResolver = mapKeyResolver;
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", mapKeyResolver.provider());
        result.put("apiKey", mapKeyResolver.jsKey());
        return Result.ok(result);
    }
}
