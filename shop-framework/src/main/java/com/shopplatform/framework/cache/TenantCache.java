package com.shopplatform.framework.cache;

import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 租户前缀缓存封装。见文档三 §2.5：
 * key 由框架统一生成，业务侧无法绕过，格式固定为 {@code t:{shopId}:{bizKey}}。
 * <p>
 * 不直接暴露 RedisTemplate 给业务代码调用，就是为了不给"手写 key 忘记加前缀"这条路留口子。
 */
@Component
public class TenantCache {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyAdmin redisKeyAdmin;

    public TenantCache(StringRedisTemplate redisTemplate, RedisKeyAdmin redisKeyAdmin) {
        this.redisTemplate = redisTemplate;
        this.redisKeyAdmin = redisKeyAdmin;
    }

    private String buildKey(String bizKey) {
        return "t:" + TenantContext.getRequired() + ":" + bizKey;
    }

    public void set(String bizKey, String value, Duration ttl) {
        redisTemplate.opsForValue().set(buildKey(bizKey), value, ttl);
    }

    public String get(String bizKey) {
        return redisTemplate.opsForValue().get(buildKey(bizKey));
    }

    public void delete(String bizKey) {
        redisTemplate.delete(buildKey(bizKey));
    }

    /** 按租户维度批量清理某一类业务前缀，供运维中心"按租户清缓存"功能使用（文档二 §1.9）。 */
    public void deleteByPrefix(String bizKeyPrefix) {
        redisKeyAdmin.deleteByPattern(buildKey(bizKeyPrefix) + "*");
    }
}
