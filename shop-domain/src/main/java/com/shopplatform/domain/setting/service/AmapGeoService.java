package com.shopplatform.domain.setting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用高德 Web 服务 Key 做逆地理和周边 POI。
 * JS API 的 Geocoder 在部分环境下会静默失败，选点后只剩经纬度；服务端调用更稳。
 */
@Service
public class AmapGeoService {

    private static final Logger log = LoggerFactory.getLogger(AmapGeoService.class);

    private final MapKeyResolver mapKeyResolver;
    private final ObjectMapper objectMapper;
    private final RestClient rest;
    private final String platformWebKey;
    private final String platformJsKey;
    private final String platformJsSecret;

    public AmapGeoService(MapKeyResolver mapKeyResolver,
                          ObjectMapper objectMapper,
                          @Value("${shop.map.amap-web-key:}") String platformWebKey,
                          @Value("${shop.map.amap-js-key:}") String platformJsKey,
                          @Value("${shop.map.amap-js-secret:}") String platformJsSecret) {
        this.mapKeyResolver = mapKeyResolver;
        this.objectMapper = objectMapper;
        this.platformWebKey = trim(platformWebKey);
        this.platformJsKey = trim(platformJsKey);
        this.platformJsSecret = trim(platformJsSecret);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(6));
        this.rest = RestClient.builder()
                .baseUrl("https://restapi.amap.com")
                .requestFactory(factory)
                .build();
    }

    public Map<String, Object> lookup(double longitude, double latitude) {
        JsonNode root = requestRegeo(longitude, latitude);
        JsonNode regeo = root.path("regeocode");
        JsonNode ac = regeo.path("addressComponent");
        String province = text(ac.get("province"));
        String city = text(ac.get("city"));
        if (!StringUtils.hasText(city)) city = province;
        String district = text(ac.get("district"));
        String township = text(ac.get("township"));
        JsonNode streetNumber = ac.path("streetNumber");
        String street = text(streetNumber.get("street"));
        String number = text(streetNumber.get("number"));
        String formatted = text(regeo.get("formatted_address"));
        List<Map<String, Object>> pois = new ArrayList<>();
        JsonNode poiNode = regeo.path("pois");
        if (poiNode.isArray()) {
            for (JsonNode poi : poiNode) {
                double[] loc = parseLocation(text(poi.get("location")));
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", text(poi.get("name")));
                row.put("address", joinAddress(province, city, district, text(poi.get("address"))));
                row.put("distance", text(poi.get("distance")));
                if (loc != null) {
                    row.put("longitude", loc[0]);
                    row.put("latitude", loc[1]);
                } else {
                    row.put("longitude", longitude);
                    row.put("latitude", latitude);
                }
                pois.add(row);
                if (pois.size() >= 12) break;
            }
        }
        String name = pois.isEmpty() ? firstNonBlank(street + number, township, district) : String.valueOf(pois.get(0).get("name"));
        String detail = firstNonBlank(joinStreet(street, number), township, formatted);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("address", formatted);
        result.put("detail", stripRegion(detail, province, city, district));
        result.put("province", province);
        result.put("city", city);
        result.put("district", district);
        result.put("region", district);
        result.put("street", street);
        result.put("streetNumber", number);
        result.put("longitude", longitude);
        result.put("latitude", latitude);
        result.put("pois", pois);
        return result;
    }

    private JsonNode requestRegeo(double longitude, double latitude) {
        String location = longitude + "," + latitude;
        JsonNode web = get("/v3/geocode/regeo", Map.of(
                "key", webKey(),
                "location", location,
                "extensions", "all",
                "radius", "1000",
                "output", "json"
        ));
        if (ok(web)) return web;
        JsonNode js = get("/v3/geocode/regeo", Map.of(
                "key", platformJsKey,
                "jscode", platformJsSecret,
                "location", location,
                "extensions", "all",
                "radius", "1000",
                "output", "json"
        ));
        if (ok(js)) return js;
        log.warn("amap regeo failed web={} js={}", info(web), info(js));
        return objectMapper.createObjectNode();
    }

    private JsonNode get(String path, Map<String, String> params) {
        try {
            StringBuilder query = new StringBuilder(path).append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!StringUtils.hasText(entry.getValue())) continue;
                if (!first) query.append('&');
                first = false;
                query.append(entry.getKey()).append('=')
                        .append(java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8));
            }
            String body = rest.get().uri(query.toString()).retrieve().body(String.class);
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (Exception e) {
            log.warn("amap request {} failed: {}", path, e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private String webKey() {
        return StringUtils.hasText(platformWebKey) ? platformWebKey : mapKeyResolver.jsKey();
    }

    private static boolean ok(JsonNode node) {
        return node != null && "1".equals(node.path("status").asText());
    }

    private static String info(JsonNode node) {
        if (node == null || node.isMissingNode()) return "empty";
        return node.path("info").asText("unknown");
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.isArray()) return "";
        return node.asText("").trim();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return "";
    }

    private static String joinStreet(String street, String number) {
        return (street + number).trim();
    }

    private static String joinAddress(String province, String city, String district, String rest) {
        String prefix = province + (city.equals(province) ? "" : city) + district;
        if (!StringUtils.hasText(rest)) return prefix;
        if (rest.startsWith(prefix)) return rest;
        return prefix + rest;
    }

    private static String stripRegion(String detail, String province, String city, String district) {
        String value = detail == null ? "" : detail.trim();
        for (String part : new String[]{province, city, district}) {
            if (StringUtils.hasText(part) && value.startsWith(part)) value = value.substring(part.length()).trim();
        }
        return value;
    }

    private static double[] parseLocation(String location) {
        if (!StringUtils.hasText(location) || !location.contains(",")) return null;
        String[] parts = location.split(",");
        try {
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
