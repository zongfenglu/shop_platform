package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.stats.DailyReportService;
import com.shopplatform.domain.stats.DashboardStatsService;
import com.shopplatform.domain.stats.entity.StatShopDaily;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 商户首页看板交易概况。对应原型 store/dashboard.html。 */
@RestController
@RequestMapping("/store/dashboard")
public class StoreDashboardController {

    private final DashboardStatsService dashboardStatsService;
    private final DailyReportService dailyReportService;

    public StoreDashboardController(DashboardStatsService dashboardStatsService,
                                    DailyReportService dailyReportService) {
        this.dashboardStatsService = dashboardStatsService;
        this.dailyReportService = dailyReportService;
    }

    @GetMapping("/overview")
    public Result<DashboardStatsService.ShopTradeOverview> overview() {
        return Result.ok(dashboardStatsService.shopOverview());
    }

    @GetMapping("/daily")
    public Result<StatShopDaily> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate statDate = date == null ? LocalDate.now().minusDays(1) : date;
        return Result.ok(dailyReportService.getByShop(TenantContext.getRequired(), statDate));
    }
}
