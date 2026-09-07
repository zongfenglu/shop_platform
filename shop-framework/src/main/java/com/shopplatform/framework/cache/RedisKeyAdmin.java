package com.shopplatform.framework.cache;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 运维侧 Redis 扫描/删除。业务读写仍走 {@link TenantCache}。
 * 不用 {@code KEYS}，避免堵 Redis。
 */
@Component
public class RedisKeyAdmin {

    private final StringRedisTemplate redisTemplate;

    public RedisKeyAdmin(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long count(String pattern) {
        long n = 0;
        try (Cursor<String> cursor = scan(pattern)) {
            while (cursor.hasNext()) {
                cursor.next();
                n++;
            }
        }
        return n;
    }

    public long deleteByPattern(String pattern) {
        long deleted = 0;
        List<String> batch = new ArrayList<>(200);
        try (Cursor<String> cursor = scan(pattern)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 200) {
                    redisTemplate.delete(batch);
                    deleted += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            redisTemplate.delete(batch);
            deleted += batch.size();
        }
        return deleted;
    }

    public MemoryInfo memory() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            return new MemoryInfo("0B", "未限制", 0L, 0L);
        }
        try (RedisConnection conn = factory.getConnection()) {
            Properties mem = conn.serverCommands().info("memory");
            Properties stats = conn.serverCommands().info("stats");
            return new MemoryInfo(
                    first(mem, "used_memory_human", "0B"),
                    emptyToUnlimited(first(mem, "maxmemory_human", "0B")),
                    parseLong(first(stats, "keyspace_hits", "0")),
                    parseLong(first(stats, "keyspace_misses", "0"))
            );
        }
    }

    private Cursor<String> scan(String pattern) {
        return redisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(200).build());
    }

    private static String first(Properties props, String key, String fallback) {
        if (props == null) {
            return fallback;
        }
        String v = props.getProperty(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static String emptyToUnlimited(String human) {
        if ("0B".equalsIgnoreCase(human) || "0".equals(human)) {
            return "未限制";
        }
        return human;
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public record MemoryInfo(String usedMemory, String maxMemory, long hits, long misses) {
        public String hitRate() {
            long total = hits + misses;
            if (total <= 0) {
                return "—";
            }
            return String.format("%.1f%%", hits * 100.0 / total);
        }
    }
}
