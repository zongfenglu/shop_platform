package com.shopplatform.domain.stats;

import com.shopplatform.domain.stats.entity.StatShopDaily;

import java.time.LocalDate;
import java.util.List;

/** T+1 日报。对应文档三 §6「每天 01:30 写 stat_*_daily」。uv/pv 无埋点时写 0。 */
public interface DailyReportService {

    int summarizeYesterday();

    int summarize(LocalDate statDate);

    List<StatShopDaily> listByDate(LocalDate statDate);

    StatShopDaily getByShop(Long shopId, LocalDate statDate);
}
