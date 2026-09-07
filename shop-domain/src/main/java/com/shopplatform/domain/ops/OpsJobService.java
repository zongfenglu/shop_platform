package com.shopplatform.domain.ops;

import java.time.LocalDateTime;
import java.util.List;

/** 运维任务目录与手动触发。已实现的任务可由超管立即执行；未实现的只展示排期。 */
public interface OpsJobService {

    List<JobInfo> list();

    JobRunResult run(String code);

    record JobInfo(
            String code,
            String name,
            String cron,
            boolean implemented,
            boolean runnable,
            String lastStatus,
            String lastMessage,
            Long lastDurationMs,
            LocalDateTime lastRunAt
    ) {
    }

    record JobRunResult(
            String code,
            String status,
            String message,
            long durationMs,
            LocalDateTime finishedAt
    ) {
    }
}
