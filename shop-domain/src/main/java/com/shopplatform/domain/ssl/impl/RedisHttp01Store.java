package com.shopplatform.domain.ssl.impl;

import com.shopplatform.domain.ssl.AcmeIssuer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisHttp01Store implements AcmeIssuer.Http01Publisher {

    private static final String PREFIX = "acme:http01:";

    private final StringRedisTemplate redisTemplate;

    public RedisHttp01Store(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void publish(String token, String authorization) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(authorization)) {
            return;
        }
        redisTemplate.opsForValue().set(PREFIX + token, authorization, Duration.ofMinutes(15));
    }

    @Override
    public void clear(String token) {
        if (StringUtils.hasText(token)) {
            redisTemplate.delete(PREFIX + token);
        }
    }

    public Optional<String> find(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        String v = redisTemplate.opsForValue().get(PREFIX + token);
        return StringUtils.hasText(v) ? Optional.of(v) : Optional.empty();
    }
}
