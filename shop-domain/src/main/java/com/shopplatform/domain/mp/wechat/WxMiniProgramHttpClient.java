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
import java.util.Map;

@Component
public class WxMiniProgramHttpClient implements WxMiniProgramClient {

    private static final Logger log = LoggerFactory.getLogger(WxMiniProgramHttpClient.class);

    private final RestClient rest;
    private final ObjectMapper objectMapper;

    public WxMiniProgramHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.rest = RestClient.builder()
                .baseUrl("https://api.weixin.qq.com")
                .requestFactory(factory)
                .build();
    }

    @Override
    public Session code2Session(String appId, String appSecret, String code) {
        JsonNode node = get("/sns/jscode2session", Map.of(
                "appid", appId,
                "secret", appSecret,
                "js_code", code,
                "grant_type", "authorization_code"
        ));
        return session(node);
    }

    @Override
    public Session componentCode2Session(String appId, String code, String componentAppId,
                                         String componentAccessToken) {
        JsonNode node = get("/sns/component/jscode2session", Map.of(
                "appid", appId,
                "js_code", code,
                "grant_type", "authorization_code",
                "component_appid", componentAppId,
                "component_access_token", componentAccessToken
        ));
        return session(node);
    }

    @Override
    public Token accessToken(String appId, String appSecret) {
        JsonNode node = get("/cgi-bin/token", Map.of(
                "grant_type", "client_credential",
                "appid", appId,
                "secret", appSecret
        ));
        return new Token(requiredText(node, "access_token"), node.path("expires_in").asInt(7200));
    }

    @Override
    public String phoneNumber(String accessToken, String code) {
        JsonNode node = post("/wxa/business/getuserphonenumber", Map.of("access_token", accessToken),
                Map.of("code", code));
        JsonNode phoneInfo = node.path("phone_info");
        String phone = phoneInfo.path("purePhoneNumber").asText("");
        if (phone.isBlank()) {
            phone = phoneInfo.path("phoneNumber").asText("");
        }
        if (phone.isBlank()) {
            throw new BusinessException(ErrorCode.WECHAT_OPEN_API_ERROR, "微信返回缺少手机号");
        }
        return phone;
    }

    private Session session(JsonNode node) {
        return new Session(requiredText(node, "openid"), node.path("unionid").asText(""));
    }

    private JsonNode get(String path, Map<String, String> query) {
        try {
            String raw = rest.get()
                    .uri(builder -> {
                        builder.path(path);
                        query.forEach(builder::queryParam);
                        return builder.build();
                    })
                    .retrieve()
                    .body(String.class);
            return parse(path, raw);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw unavailable(path, e);
        }
    }

    private JsonNode post(String path, Map<String, String> query, Map<String, Object> body) {
        try {
            String raw = rest.post()
                    .uri(builder -> {
                        builder.path(path);
                        query.forEach(builder::queryParam);
                        return builder.build();
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
            return parse(path, raw);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw unavailable(path, e);
        }
    }

    private JsonNode parse(String path, String raw) throws Exception {
        JsonNode node = objectMapper.readTree(raw == null ? "{}" : raw);
        int errCode = node.path("errcode").asInt(0);
        if (errCode != 0) {
            String errMsg = node.path("errmsg").asText("errcode=" + errCode);
            log.warn("微信小程序接口返回错误 path={} errcode={} errmsg={}", path, errCode, errMsg);
            throw new BusinessException(ErrorCode.WECHAT_OPEN_API_ERROR, "微信接口：" + errMsg);
        }
        return node;
    }

    private BusinessException unavailable(String path, Exception e) {
        log.warn("调用微信小程序接口失败 path={} msg={}", path, e.getMessage());
        return new BusinessException(ErrorCode.WECHAT_OPEN_API_ERROR,
                e.getMessage() == null ? "无法连接微信接口" : e.getMessage());
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.isBlank()) {
            throw new BusinessException(ErrorCode.WECHAT_OPEN_API_ERROR, "微信返回缺少 " + field);
        }
        return value;
    }
}
