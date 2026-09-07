package com.shopplatform.adminapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.stats.DashboardStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 平台数据看板交易概况。对应原型 admin/dashboard.html。 */
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final DashboardStatsService dashboardStatsService;

    public AdminDashboardController(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = dashboardStatsService;
    }

    @GetMapping("/overview")
    public Result<DashboardStatsService.PlatformTradeOverview> overview() {
        return Result.ok(dashboardStatsService.platformOverview());
    }
}
