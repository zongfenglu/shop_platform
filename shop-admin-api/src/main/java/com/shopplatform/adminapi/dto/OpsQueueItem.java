package com.shopplatform.adminapi.dto;

public record OpsQueueItem(
        String name,
        String topic,
        String consumerGroup,
        boolean implemented,
        Long backlog,
        Long deadLetter,
        String message
) {
}
