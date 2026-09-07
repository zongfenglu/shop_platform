package com.shopplatform.domain.mp.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WxOpenHttpClient implements WxOpenPlatformClient {

    private static final Logger log = LoggerFactory.getLogger(WxOpenHttpClient.class);
    private static final String BASE = "https://api.weixin.qq.com/cgi-bin/component";

    private final RestClient rest;
    private final ObjectMapper objectMapper;

    public WxOpenHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.rest = RestClient.builder().baseUrl(BASE).requestFactory(factory).build();
    }

    @Override
    public Token componentToken(String componentAppId, String componentSecret, String ticket) {
        JsonNode node = post("/api_component_token", Map.of(
                "component_appid", componentAppId,
                "component_appsecret", componentSecret,
                "component_verify_ticket", ticket
        ));
        return new Token(text(node, "component_access_token"), node.path("expires_in").asInt(7200));
    }

    @Override
    public String createPreAuthCode(String componentAccessToken, String componentAppId) {
        JsonNode node = post("/api_create_preauthcode?component_access_token=" + componentAccessToken,
                Map.of("component_appid", componentAppId));
        return text(node, "pre_auth_code");
    }

    @Override
    public Authorization queryAuth(String componentAccessToken, String componentAppId, String authCode) {
        JsonNode node = post("/api_query_auth?component_access_token=" + componentAccessToken, Map.of(
                "component_appid", componentAppId,
                "authorization_code", authCode
        ));
        JsonNode info = node.path("authorization_info");
        return new Authorization(
                text(info, "authorizer_appid"),
                info.path("authorizer_access_token").asText(""),
                info.path("expires_in").asInt(7200),
                text(info, "authorizer_refresh_token"),
                info.path("func_info").toString()
        );
    }

    @Override
    public Token refreshAuthorizerToken(String componentAccessToken, String componentAppId,
                                        String authorizerAppId, String refreshToken) {
        JsonNode node = post("/api_authorizer_token?component_access_token=" + componentAccessToken, Map.of(
                "component_appid", componentAppId,
                "authorizer_appid", authorizerAppId,
                "authorizer_refresh_token", refreshToken
        ));
        return new Token(text(node, "authorizer_access_token"), node.path("expires_in").asInt(7200));
    }

    @Override
    public AuthorizerInfo getAuthorizerInfo(String componentAccessToken, String componentAppId, String authorizerAppId) {
        JsonNode node = post("/api_get_authorizer_info?component_access_token=" + componentAccessToken, Map.of(
                "component_appid", componentAppId,
                "authorizer_appid", authorizerAppId
        ));
        JsonNode info = node.path("authorizer_info");
        List<Integer> ids = new ArrayList<>();
        for (JsonNode item : node.path("authorization_info").path("func_info")) {
            ids.add(item.path("funcscope_category").path("id").asInt());
        }
        return new AuthorizerInfo(
                info.path("nick_name").asText(""),
                info.path("appid").asText(authorizerAppId),
                info.path("user_name").asText(""),
                ids
        );
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            String raw = rest.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(raw == null ? "{}" : raw);
            int err = node.path("errcode").asInt(0);
            if (err != 0) {
                log.warn("微信开放平台返回错误 path={} errcode={} errmsg={}", path, err, node.path("errmsg").asText());
                throw new BusinessException(ErrorCode.WECHAT_OPEN_API_ERROR,
                        "微信开放平台：" + node.path("errmsg").asText("errcode=" + err));
            }
            return node;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用微信开放平台失败 path={} msg={}", path, e.getMessage());
            throw new BusinessException(ErrorCode.WECHAT_OPEN_API_ERROR,
                    e.getMessage() == null ? "无法连接微信开放平台" : e.getMessage());
        }
    }

    private static String text(JsonNode node, String field) {
        String v = node.path(field).asText("");
        if (v.isBlank()) {
            throw new BusinessException(ErrorCode.WECHAT_OPEN_API_ERROR, "微信返回缺少 " + field);
        }
        return v;
    }

    @SuppressWarnings("unused")
    private static Map<String, Object> empty() {
        return new LinkedHashMap<>();
    }
}
