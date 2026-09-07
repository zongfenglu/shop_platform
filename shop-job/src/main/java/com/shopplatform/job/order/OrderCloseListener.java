package com.shopplatform.job.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.mq.OrderCloseDelayProducer.OrderCloseMessage;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.shopplatform.domain.order.mq.OrderCloseDelayProducer.TAG;
import static com.shopplatform.domain.order.mq.OrderCloseDelayProducer.TOPIC;

/**
 * 超时关单延时消息消费者。见文档三 §5："消费时校验订单仍为待付款 -> 关单回滚库存"——
 * 消费到消息时重新查一次订单当前状态，不信任消息体携带的下单时状态快照，
 * 因为延时消息可能在数分钟后才被消费，这段时间内订单完全可能已经支付成功。
 * <p>
 * {@code order-close:timeout} 只有一条消费者组，天然满足"每条消息只处理一次"的正常预期；
 * 即使消息重复投递，{@link OrderService#cancel} 内部用 {@code WHERE pay_status='unpaid'} 乐观锁
 * 兜底，重复消费也不会二次回补库存。
 */
@Component
@ConditionalOnProperty(name = "shop.rocketmq.enabled", havingValue = "true")
@RocketMQMessageListener(topic = TOPIC, selectorExpression = TAG,
        consumerGroup = "shop-job-order-close", consumeMode = ConsumeMode.CONCURRENTLY)
public class OrderCloseListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseListener.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderCloseListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String body) {
        OrderCloseMessage message;
        try {
            message = objectMapper.readValue(body, OrderCloseMessage.class);
        } catch (Exception e) {
            log.error("超时关单消息反序列化失败，丢弃: {}", body, e);
            return;
        }

        TenantContext.set(message.shopId());
        try {
            Order order = orderService.getByIdWithTenant(message.orderId());
            boolean cancelled = orderService.cancel(message.orderId(), "超时未支付自动取消");
            log.info("超时关单处理完成 orderId={} orderNo={} cancelled={}",
                    message.orderId(), order.getOrderNo(), cancelled);
        } finally {
            TenantContext.clear();
        }
    }
}
