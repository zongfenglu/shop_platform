package com.shopplatform.domain.ops.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseBackupServiceImplTest {

    @Test
    void literal_escapesAndFormats() {
        assertEquals("NULL", DatabaseBackupServiceImpl.literal(null));
        assertEquals("12", DatabaseBackupServiceImpl.literal(12));
        assertEquals("'O''Reilly'", DatabaseBackupServiceImpl.literal("O'Reilly"));
        assertEquals("'2026-08-30 01:02:03'", DatabaseBackupServiceImpl.literal(LocalDateTime.of(2026, 8, 30, 1, 2, 3)));
        assertEquals("2680.00", DatabaseBackupServiceImpl.literal(new BigDecimal("2680.00")));
    }
}
