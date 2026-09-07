package com.shopplatform.domain.ops.impl;

import com.shopplatform.domain.ops.QueueMetricsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueMetricsServiceImplTest {

    @Test
    void emptyNameServerReturnsHonestUnimplementedQueues() {
        QueueMetricsService.Overview overview = new QueueMetricsServiceImpl("").overview();
        assertFalse(overview.reachable());
        assertEquals(4, overview.queues().size());

        QueueMetricsService.QueueStat order = overview.queues().get(0);
        assertTrue(order.implemented());
        assertEquals("order-close", order.topic());
        assertEquals(0L, order.backlog());

        QueueMetricsService.QueueStat sms = overview.queues().get(1);
        assertFalse(sms.implemented());
        assertNull(sms.backlog());
        assertTrue(sms.message().contains("尚未接入"));
    }
}
