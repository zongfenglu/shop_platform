package com.shopplatform.domain.ops;

import java.util.List;

public interface QueueMetricsService {

    Overview overview();

    record Overview(boolean reachable, String nameServer, String message, List<QueueStat> queues) {
    }

    record QueueStat(
            String name,
            String topic,
            String consumerGroup,
            boolean implemented,
            Long backlog,
            Long deadLetter,
            String message
    ) {
    }
}
