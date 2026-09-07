package com.shopplatform.app;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** 本地默认关掉 MQ；生产拆分部署或显式打开时再装配。 */
@Configuration
@ConditionalOnProperty(name = "shop.rocketmq.enabled", havingValue = "true")
@Import(RocketMQAutoConfiguration.class)
public class OptionalRocketMqConfig {
}
