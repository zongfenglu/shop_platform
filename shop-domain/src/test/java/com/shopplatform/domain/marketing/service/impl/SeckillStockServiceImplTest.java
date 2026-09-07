package com.shopplatform.domain.marketing.service.impl;

import com.shopplatform.domain.marketing.service.SeckillStockService;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SeckillStockServiceImpl} 单测：预扣结果码翻译、回补、初始化/对账、key 含租户前缀。
 * Lua 脚本本身的原子性由 Redis 保证，这里只验证服务层把脚本返回值翻译成业务码、key 拼接正确。
 */
class SeckillStockServiceImplTest {

    @SuppressWarnings("unchecked")
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    private SeckillStockService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(1001L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new SeckillStockServiceImpl(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void preDeduct_success_returnsSuccess() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
        assertEquals(SeckillStockService.SUCCESS, service.preDeduct(9L, 1L, 11L, 2, 1));
    }

    @Test
    void preDeduct_stockInsufficient_returnsNeg1() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(-1L);
        assertEquals(SeckillStockService.STOCK_INSUFFICIENT, service.preDeduct(9L, 1L, 11L, 2, 1));
    }

    @Test
    void preDeduct_limitExceeded_returnsNeg2() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(-2L);
        assertEquals(SeckillStockService.LIMIT_EXCEEDED, service.preDeduct(9L, 1L, 11L, 2, 1));
    }

    @Test
    void rollback_incrementsStockAndDecrementsBought() {
        service.rollback(9L, 1L, 11L, 2);
        verify(valueOps).increment("t:1001:seckill:stock:1:11", 2);
        verify(valueOps).increment("t:1001:seckill:bought:1:11:9", -2);
    }

    @Test
    void initStock_setsStockKey() {
        service.initStock(1L, 11L, 100);
        verify(valueOps).set(eq("t:1001:seckill:stock:1:11"), eq("100"));
    }

    @Test
    void reconcile_setsStockToNumMinusSold() {
        service.reconcile(1L, 11L, 100, 30);
        verify(valueOps).set(eq("t:1001:seckill:stock:1:11"), eq("70"));
    }

    @Test
    void reconcile_soldExceedsNum_clampsToZero() {
        service.reconcile(1L, 11L, 100, 130);
        verify(valueOps).set(eq("t:1001:seckill:stock:1:11"), eq("0"));
    }

    @Test
    void evictStock_deletesStockKey() {
        service.evictStock(1L, 11L);
        verify(redisTemplate).delete("t:1001:seckill:stock:1:11");
    }
}
