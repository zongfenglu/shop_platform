package com.shopplatform.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 生成与解析。三套鉴权主体（platform / store / user）共用同一套工具类，
 * 差异体现在 claims 内容上：
 * - platform：{@code sub=adminId}
 * - store：{@code sub=storeUserId, shopId=xxx}
 * - user（消费者）：{@code sub=userId, shopId=xxx}
 * <p>
 * 见文档三 §8 接口与鉴权规范。
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expireMillis;

    public JwtTokenProvider(
            @Value("${shop.jwt.secret:CHANGE_ME_IN_PRODUCTION_ENV_this_is_a_dev_only_default_secret_key}") String secret,
            @Value("${shop.jwt.expire-hours:12}") long expireHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600 * 1000;
    }

    public String generate(String subject, Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析失败（过期/篡改/格式错误）统一抛出运行时异常，由调用方转为 401。 */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
