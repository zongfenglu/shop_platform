package com.shopplatform.domain.setting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 用高德 Web 服务 Key 做搜索、逆地理和周边 POI。
 * Web 端 JS Key 只能出底图，restapi 会返回 INVALID_USER_KEY；Web 服务 Key 若开了数字签名必须带 sig。
 */
@Service
public class AmapGeoService {

    private static final Logger log = LoggerFactory.getLogger(AmapGeoService.class);

    private final ObjectMapper objectMapper;
    private final RestClient rest;
    private final String platformWebKey;
    private final String platformWebSecret;

    public AmapGeoService(ObjectMapper objectMapper,
                          @Value("${shop.map.amap-web-key:}") String platformWebKey,
                          @Value("${shop.map.amap-web-secret:}") String platformWebSecret) {
        this.objectMapper = objectMapper;
        this.platformWebKey = trim(platformWebKey);
        this.platformWebSecret = trim(platformWebSecret);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(6));
        this.rest = RestClient.builder()
                .baseUrl("https://restapi.amap.com")
                .requestFactory(factory)
                .build();
    }

    public Map<String, Object> lookup(double longitude, double latitude) {
        String location = longitude + "," + latitude;
        JsonNode root = get("/v3/geocode/regeo", Map.of(
                "location", location,
                "extensions", "all",
                "radius", "1000"
        ));
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
        List<Map<String, Object>> pois = parsePois(regeo.path("pois"), longitude, latitude, province, city, district);
        if (pois.isEmpty()) {
            JsonNode around = get("/v3/place/around", Map.of(
                    "location", location,
                    "radius", "800",
                    "offset", "12",
                    "extensions", "all"
            ));
            pois = parsePois(around.path("pois"), longitude, latitude, province, city, district);
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
        if (!StringUtils.hasText(formatted) && pois.isEmpty()) {
            result.put("error", authHint(root));
        }
        return result;
    }

    public List<Map<String, Object>> search(String keyword) {
        if (!StringUtils.hasText(keyword)) return List.of();
        JsonNode root = get("/v3/place/text", Map.of(
                "keywords", keyword.trim(),
                "offset", "12",
                "extensions", "all"
        ));
        List<Map<String, Object>> pois = parsePois(root.path("pois"), 0, 0, "", "", "");
        if (pois.isEmpty() && !ok(root)) {
            log.warn("amap place search failed keyword={} info={}", keyword, info(root));
        }
        return pois;
    }

    private List<Map<String, Object>> parsePois(JsonNode poiNode, double longitude, double latitude,
                                                String province, String city, String district) {
        List<Map<String, Object>> pois = new ArrayList<>();
        if (!poiNode.isArray()) return pois;
        for (JsonNode poi : poiNode) {
            double[] loc = parseLocation(text(poi.get("location")));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", text(poi.get("name")));
            row.put("address", firstNonBlank(text(poi.get("address")), joinAddress(province, city, district, "")));
            row.put("distance", text(poi.get("distance")));
            if (loc != null) {
                row.put("longitude", loc[0]);
                row.put("latitude", loc[1]);
            } else {
                row.put("longitude", longitude);
                row.put("latitude", latitude);
            }
            JsonNode pname = poi.get("pname");
            JsonNode cityname = poi.get("cityname");
            JsonNode adname = poi.get("adname");
            if (pname != null) row.put("province", text(pname));
            if (cityname != null) row.put("city", text(cityname));
            if (adname != null) row.put("district", text(adname));
            pois.add(row);
            if (pois.size() >= 12) break;
        }
        return pois;
    }

    private JsonNode get(String path, Map<String, String> params) {
        if (!StringUtils.hasText(platformWebKey)) {
            log.warn("amap web key is empty");
            return objectMapper.createObjectNode();
        }
        try {
            TreeMap<String, String> signed = new TreeMap<>();
            params.forEach((key, value) -> {
                if (StringUtils.hasText(value)) signed.put(key, value);
            });
            signed.put("key", platformWebKey);
            signed.put("output", "json");
            if (StringUtils.hasText(platformWebSecret)) {
                signed.put("sig", sign(signed, platformWebSecret));
            }
            StringBuilder query = new StringBuilder(path).append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : signed.entrySet()) {
                if (!first) query.append('&');
                first = false;
                query.append(entry.getKey()).append('=')
                        .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            String body = rest.get().uri(query.toString()).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            if (!ok(root)) {
                log.warn("amap {} failed info={}", path, info(root));
            }
            return root;
        } catch (Exception e) {
            log.warn("amap request {} failed: {}", path, e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private static String sign(TreeMap<String, String> params, String secret) {
        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if ("sig".equals(entry.getKey()) || !StringUtils.hasText(entry.getValue())) continue;
            if (!raw.isEmpty()) raw.append('&');
            raw.append(entry.getKey()).append('=').append(entry.getValue());
        }
        raw.append(secret);
        return DigestUtils.md5DigestAsHex(raw.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String authHint(JsonNode node) {
        String code = info(node);
        if ("INVALID_USER_SIGNATURE".equals(code)) {
            return "WEB_KEY_SIGNATURE";
        }
        if ("INVALID_USER_KEY".equals(code) || "USERKEY_PLAT_NOMATCH".equals(code)) {
            return "WEB_KEY_INVALID";
        }
        if ("INVALID_USER_IP".equals(code)) {
            return "WEB_KEY_IP";
        }
        return StringUtils.hasText(code) ? code : "EMPTY";
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
