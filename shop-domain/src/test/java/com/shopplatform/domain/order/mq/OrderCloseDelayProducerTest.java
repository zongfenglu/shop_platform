package com.shopplatform.domain.order.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderCloseDelayProducerTest {

    @Test
    void sendDelayClose_noopWhenRocketMqAbsent() {
        @SuppressWarnings("unchecked")
        ObjectProvider<org.apache.rocketmq.spring.core.RocketMQTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        ObjectMapper mapper = mock(ObjectMapper.class);

        new OrderCloseDelayProducer(provider, mapper).sendDelayClose(1001L, 9L, 30);

        verifyNoInteractions(mapper);
    }
}
