package com.shopplatform.domain.ops.impl;

import com.shopplatform.domain.ops.QueueMetricsService;
import com.shopplatform.domain.order.mq.OrderCloseDelayProducer;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper;
import org.apache.rocketmq.remoting.protocol.admin.TopicOffset;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QueueMetricsServiceImpl implements QueueMetricsService {

    private static final Logger log = LoggerFactory.getLogger(QueueMetricsServiceImpl.class);
    private static final String ORDER_GROUP = "shop-job-order-close";

    private final String nameServer;

    public QueueMetricsServiceImpl(@Value("${rocketmq.name-server:}") String nameServer) {
        this.nameServer = nameServer;
    }

    @Override
    public Overview overview() {
        List<QueueStat> queues = new ArrayList<>();
        if (!StringUtils.hasText(nameServer)) {
            queues.add(planned(OrderCloseDelayProducer.TOPIC, ORDER_GROUP, true, "未配置 NameServer"));
            queues.addAll(plannedOthers());
            return new Overview(false, "", "未配置 rocketmq.name-server", queues);
        }
        DefaultMQAdminExt admin = new DefaultMQAdminExt();
        admin.setNamesrvAddr(nameServer);
        admin.setInstanceName("ops-queue-" + System.nanoTime());
        admin.setVipChannelEnabled(false);
        try {
            admin.start();
            queues.add(orderClose(admin));
            queues.addAll(plannedOthers());
            return new Overview(true, nameServer, "NameServer 可达", queues);
        } catch (Exception e) {
            log.warn("查询 RocketMQ 积压失败: {}", e.getMessage());
            queues.add(planned(OrderCloseDelayProducer.TOPIC, ORDER_GROUP, true,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            queues.addAll(plannedOthers());
            return new Overview(false, nameServer, "无法连接 NameServer", queues);
        } finally {
            try {
                admin.shutdown();
            } catch (Exception ignored) {
                // 查询失败时 start 可能没成功
            }
        }
    }

    private QueueStat orderClose(DefaultMQAdminExt admin) {
        Long backlog = null;
        String message = "已接入超时关单延时消息";
        try {
            ConsumeStats stats = admin.examineConsumeStats(ORDER_GROUP, OrderCloseDelayProducer.TOPIC);
            backlog = diff(stats);
        } catch (Exception e) {
            message = "消费组尚未产生位点（还没有关单消息）";
            backlog = 0L;
        }
        long dead = deadLetters(admin, ORDER_GROUP);
        return new QueueStat(
                "order-timeout-queue",
                OrderCloseDelayProducer.TOPIC,
                ORDER_GROUP,
                true,
                backlog,
                dead,
                message
        );
    }

    private static long diff(ConsumeStats stats) {
        if (stats == null || stats.getOffsetTable() == null) {
            return 0L;
        }
        long n = 0;
        for (Map.Entry<MessageQueue, OffsetWrapper> e : stats.getOffsetTable().entrySet()) {
            OffsetWrapper off = e.getValue();
            if (off == null) {
                continue;
            }
            n += Math.max(0, off.getBrokerOffset() - off.getConsumerOffset());
        }
        return n;
    }

    private static long deadLetters(DefaultMQAdminExt admin, String group) {
        try {
            TopicStatsTable table = admin.examineTopicStats("%DLQ%" + group);
            if (table == null || table.getOffsetTable() == null) {
                return 0L;
            }
            long n = 0;
            for (TopicOffset off : table.getOffsetTable().values()) {
                if (off != null) {
                    n += Math.max(0, off.getMaxOffset() - off.getMinOffset());
                }
            }
            return n;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static List<QueueStat> plannedOthers() {
        return List.of(
                planned("sms-notify", "shop-job-sms", false, "短信发送队列尚未接入"),
                planned("mp-release", "shop-job-mp-release", false, "小程序发布队列尚未接入"),
                planned("commission-settle", "shop-job-commission", false, "佣金结算走定时任务，无独立队列")
        );
    }

    private static QueueStat planned(String topic, String group, boolean implemented, String message) {
        return new QueueStat(topic + "-queue", topic, group, implemented, implemented ? 0L : null, implemented ? 0L : null, message);
    }
}
