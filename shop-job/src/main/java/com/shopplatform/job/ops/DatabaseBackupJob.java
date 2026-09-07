package com.shopplatform.job.ops;

import com.shopplatform.domain.ops.DatabaseBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 逻辑备份当前库。对应文档二 §1.9，每天 02:00。 */
@Component
public class DatabaseBackupJob {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupJob.class);

    private final DatabaseBackupService databaseBackupService;

    public DatabaseBackupJob(DatabaseBackupService databaseBackupService) {
        this.databaseBackupService = databaseBackupService;
    }

    @Scheduled(cron = "${shop.job.db-backup-cron:0 0 2 * * ?}")
    public void runDaily() {
        log.info("数据库备份开始");
        var row = databaseBackupService.create();
        log.info("数据库备份结束 status={} file={}", row.getStatus(), row.getFilename());
    }
}
