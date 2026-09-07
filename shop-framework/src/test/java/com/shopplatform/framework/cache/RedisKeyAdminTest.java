package com.shopplatform.framework.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisKeyAdminTest {

    @Test
    void hitRate_formatsOrDash() {
        assertEquals("—", new RedisKeyAdmin.MemoryInfo("1M", "未限制", 0, 0).hitRate());
        assertEquals("80.0%", new RedisKeyAdmin.MemoryInfo("1M", "未限制", 8, 2).hitRate());
    }
}
