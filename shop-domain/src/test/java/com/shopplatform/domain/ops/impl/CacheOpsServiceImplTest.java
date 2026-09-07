package com.shopplatform.domain.ops.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheOpsServiceImplTest {

    @Test
    void patternOf_mapsScopes() {
        assertEquals("t:1001:*", CacheOpsServiceImpl.patternOf("tenant", 1001L));
        assertEquals("t:*:goods:*", CacheOpsServiceImpl.patternOf("goods", null));
        assertEquals("t:*:diy:*", CacheOpsServiceImpl.patternOf("diy", null));
        assertEquals("t:*:seckill:*", CacheOpsServiceImpl.patternOf("seckill", null));
        assertEquals("t:*", CacheOpsServiceImpl.patternOf("all", null));
    }

    @Test
    void patternOf_tenantRequiresShop() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CacheOpsServiceImpl.patternOf("tenant", null));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    void patternOf_unknownScope() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CacheOpsServiceImpl.patternOf("sms", null));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }
}
