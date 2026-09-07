package com.shopplatform.domain.order.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 超时关单延时消息生产者。见文档三 §5。
 * <p>
 * RocketMQ 未启用时（本地 all-in-one / {@code shop.rocketmq.enabled=false}）这里是空操作，
 * 关单改走 {@code OrderTimeoutJob} 每 5 分钟扫漏单，避免为延时消息再起两个 JVM。
 */
@Component
public class OrderCloseDelayProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseDelayProducer.class);

    public static final String TOPIC = "order-close";
    public static final String TAG = "timeout";

    /** RocketMQ 只支持固定的18个延时级别（1s~2h），支付超时关单场景够用；分钟数精确到级别边界，见 delayLevelFor()。 */
    private static final int[] DELAY_LEVEL_SECONDS =
            {1, 5, 10, 30, 60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 1200, 1800, 3600, 7200};

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public OrderCloseDelayProducer(ObjectProvider<RocketMQTemplate> rocketMQTemplate, ObjectMapper objectMapper) {
        this.rocketMQTemplate = rocketMQTemplate.getIfAvailable();
        this.objectMapper = objectMapper;
    }

    /**
     * 异步发送：这是下单主链路事务提交后的收尾动作，不能让消息队列的网络往返阻塞住给客户端的响应——
     * 见 M1 压测：改成同步发送前，单节点下单 QPS 被压测直接测出因为这一次同步网络调用而腰斩。
     * 失败回调只记日志，不抛异常/不重试，理由同下方注释：真正的兜底是补偿定时任务和商户手动关单。
     */
    public void sendDelayClose(Long shopId, Long orderId, int payTimeoutMinutes) {
        if (rocketMQTemplate == null) {
            log.debug("RocketMQ 未启用，超时关单交给定时任务 shopId={} orderId={}", shopId, orderId);
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(new OrderCloseMessage(shopId, orderId));
            Message<String> message = MessageBuilder.withPayload(payload).build();
            int level = delayLevelFor(payTimeoutMinutes * 60L);
            rocketMQTemplate.asyncSend(TOPIC + ":" + TAG, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    // 无需处理：发送成功是默认路径，不需要额外记录。
                }

                @Override
                public void onException(Throwable e) {
                    log.error("超时关单延时消息发送失败 shopId={} orderId={}", shopId, orderId, e);
                }
            }, 3000, level);
        } catch (Exception e) {
            // 消息发送失败不应该回滚下单事务——宁可损失一次自动关单兜底，也不能让"发消息失败"变成"下单失败"，
            // 真正的兜底还有 Sprint 5 的主动查单补偿定时任务和商户手动关单。
            log.error("超时关单延时消息发送失败 shopId={} orderId={}", shopId, orderId, e);
        }
    }

    private int delayLevelFor(long targetSeconds) {
        for (int i = 0; i < DELAY_LEVEL_SECONDS.length; i++) {
            if (DELAY_LEVEL_SECONDS[i] >= targetSeconds) {
                return i + 1;
            }
        }
        return DELAY_LEVEL_SECONDS.length;
    }

    public record OrderCloseMessage(Long shopId, Long orderId) {
    }
}
