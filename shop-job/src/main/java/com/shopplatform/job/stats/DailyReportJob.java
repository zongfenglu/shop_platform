package com.shopplatform.job.stats;

import com.shopplatform.domain.stats.DailyReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 日报汇总。对应文档三 §6「每天 01:30 写 stat_*_daily」。 */
@Component
public class DailyReportJob {

    private static final Logger log = LoggerFactory.getLogger(DailyReportJob.class);

    private final DailyReportService dailyReportService;

    public DailyReportJob(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @Scheduled(cron = "${shop.job.daily-report-cron:0 30 1 * * ?}")
    public void runDaily() {
        log.info("日报汇总开始");
        int n = dailyReportService.summarizeYesterday();
        log.info("日报汇总结束 shops={}", n);
    }
}
