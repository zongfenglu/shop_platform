package com.shopplatform.framework.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setAndGet_shouldReturnSameValue() {
        TenantContext.set(100L);
        assertEquals(100L, TenantContext.get());
    }

    @Test
    void clear_shouldRemoveShopIdAndIgnoreFlag() {
        TenantContext.set(100L);
        TenantContext.ignoreTenant(() -> null);
        TenantContext.clear();
        assertNull(TenantContext.get());
        assertFalse(TenantContext.isIgnoreTenant());
    }

    @Test
    void getRequired_withoutContext_shouldThrow() {
        assertThrows(IllegalStateException.class, TenantContext::getRequired);
    }

    @Test
    void ignoreTenant_shouldSetFlagDuringExecutionOnly() {
        assertFalse(TenantContext.isIgnoreTenant());
        TenantContext.ignoreTenant(() -> {
            assertTrue(TenantContext.isIgnoreTenant());
            return null;
        });
        assertFalse(TenantContext.isIgnoreTenant());
    }

    /**
     * 嵌套场景回归：外层已经是 ignoreTenant，内层再嵌一层 ignoreTenant 执行完毕后，
     * 应该恢复外层的状态而不是直接清空——否则会出现"内层退出后，外层的忽略状态被误关闭"的 bug。
     */
    @Test
    void ignoreTenant_nested_shouldRestorePreviousFlagNotClearIt() {
        TenantContext.ignoreTenant(() -> {
            assertTrue(TenantContext.isIgnoreTenant());
            TenantContext.ignoreTenant(() -> {
                assertTrue(TenantContext.isIgnoreTenant());
                return null;
            });
            // 内层退出后，外层的忽略状态必须还在
            assertTrue(TenantContext.isIgnoreTenant());
            return null;
        });
        assertFalse(TenantContext.isIgnoreTenant());
    }
}
