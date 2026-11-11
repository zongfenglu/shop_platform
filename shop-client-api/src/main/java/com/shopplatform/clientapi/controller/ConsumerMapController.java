package com.shopplatform.clientapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.setting.service.AmapGeoService;
import com.shopplatform.domain.setting.service.MapKeyResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 消费者端地图配置。H5 用 Web 端 JS Key + 安全密钥；小程序用微信原生地图，Key 仅作备用。 */
@RestController
@RequestMapping("/api/map")
public class ConsumerMapController {

    private final MapKeyResolver mapKeyResolver;
    private final AmapGeoService amapGeoService;

    public ConsumerMapController(MapKeyResolver mapKeyResolver, AmapGeoService amapGeoService) {
        this.mapKeyResolver = mapKeyResolver;
        this.amapGeoService = amapGeoService;
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", mapKeyResolver.provider());
        result.put("apiKey", mapKeyResolver.jsKey());
        result.put("securityJsCode", mapKeyResolver.securityJsCode());
        result.put("mpKey", mapKeyResolver.mpKey());
        return Result.ok(result);
    }

    @GetMapping("/lookup")
    public Result<Map<String, Object>> lookup(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            return Result.ok(Map.of());
        }
        return Result.ok(amapGeoService.lookup(longitude, latitude));
    }
}
