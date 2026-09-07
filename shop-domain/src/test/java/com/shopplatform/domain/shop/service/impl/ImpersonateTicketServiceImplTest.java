package com.shopplatform.domain.shop.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImpersonateTicketServiceImplTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private ImpersonateTicketServiceImpl service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        service = new ImpersonateTicketServiceImpl(redis);
    }

    @Test
    void issue_writesRedisWithFiveMinuteTtl() {
        String ticket = service.issue(1001L, 3001L);
        assertNotNull(ticket);
        verify(ops).set(eq("shop:impersonate:" + ticket), eq("1001:3001"), eq(Duration.ofSeconds(300)));
    }

    @Test
    void consume_returnsPayloadAndDeletesKey() {
        when(ops.getAndDelete("shop:impersonate:abc")).thenReturn("1001:3001");
        var payload = service.consume("abc");
        assertEquals(1001L, payload.shopId());
        assertEquals(3001L, payload.storeUserId());
    }

    @Test
    void consume_missingOrBlank_returnsNull() {
        when(ops.getAndDelete(anyString())).thenReturn(null);
        assertNull(service.consume("gone"));
        assertNull(service.consume(""));
        assertNull(service.consume(null));
    }
}
