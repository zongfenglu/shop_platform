package com.shopplatform.adminapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.adminapi.dto.DailyStatItem;
import com.shopplatform.adminapi.dto.OpsBackupItem;
import com.shopplatform.adminapi.dto.OpsCacheFlushRequest;
import com.shopplatform.adminapi.dto.OpsCacheFlushResult;
import com.shopplatform.adminapi.dto.OpsCacheSnapshot;
import com.shopplatform.adminapi.dto.OpsJobItem;
import com.shopplatform.adminapi.dto.OpsJobRunResult;
import com.shopplatform.adminapi.dto.OpsOverviewResponse;
import com.shopplatform.adminapi.dto.OpsQueueItem;
import com.shopplatform.adminapi.dto.OpsQueueOverview;
import com.shopplatform.adminapi.dto.OpsUsageItem;
import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.ops.CacheOpsService;
import com.shopplatform.domain.ops.DatabaseBackupService;
import com.shopplatform.domain.ops.OpsJobService;
import com.shopplatform.domain.ops.QueueMetricsService;
import com.shopplatform.domain.ops.entity.OpsBackup;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.platform.service.PlatformUserService;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.entity.ShopQuotaUsage;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.shop.service.ShopQuotaUsageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.stats.DailyReportService;
import com.shopplatform.domain.stats.entity.StatShopDaily;
import com.shopplatform.framework.tenant.TenantContext;
import com.shopplatform.framework.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/ops")
public class AdminOpsController {

    private final ShopService shopService;
    private final OrderService orderService;
    private final ShopDomainService shopDomainService;
    private final PlatformUserService platformUserService;
    private final ShopQuotaUsageService shopQuotaUsageService;
    private final OpsJobService opsJobService;
    private final DailyReportService dailyReportService;
    private final DatabaseBackupService databaseBackupService;
    private final CacheOpsService cacheOpsService;
    private final QueueMetricsService queueMetricsService;
    private final SysLogService sysLogService;

    public AdminOpsController(ShopService shopService,
                              OrderService orderService,
                              ShopDomainService shopDomainService,
                              PlatformUserService platformUserService,
                              ShopQuotaUsageService shopQuotaUsageService,
                              OpsJobService opsJobService,
                              DailyReportService dailyReportService,
                              DatabaseBackupService databaseBackupService,
                              CacheOpsService cacheOpsService,
                              QueueMetricsService queueMetricsService,
                              SysLogService sysLogService) {
        this.shopService = shopService;
        this.orderService = orderService;
        this.shopDomainService = shopDomainService;
        this.platformUserService = platformUserService;
        this.shopQuotaUsageService = shopQuotaUsageService;
        this.opsJobService = opsJobService;
        this.dailyReportService = dailyReportService;
        this.databaseBackupService = databaseBackupService;
        this.cacheOpsService = cacheOpsService;
        this.queueMetricsService = queueMetricsService;
        this.sysLogService = sysLogService;
    }

    @GetMapping("/overview")
    public Result<OpsOverviewResponse> overview() {
        List<Shop> shops = shopService.list();
        Map<Long, Shop> shopById = new LinkedHashMap<>();
        for (Shop shop : shops) {
            shopById.put(shop.getId(), shop);
        }

        long shopTotal = shops.size();
        long activeShopTotal = shops.stream()
                .filter(s -> ShopStatus.TRIAL.getCode().equals(s.getStatus()) || ShopStatus.NORMAL.getCode().equals(s.getStatus()))
                .count();
        long pendingDomainTotal = shopDomainService.count(
                Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getVerifyStatus, "pending"));
        long orderTotal = TenantContext.ignoreTenant((Supplier<Long>) orderService::count);
        long platformUserTotal = platformUserService.count();

        List<ShopQuotaUsage> usageRows = shopQuotaUsageService.listLatestSnapshots(5);
        usageRows.sort(Comparator.comparing(ShopQuotaUsage::getStatDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<OpsUsageItem> usageItems = new ArrayList<>();
        long totalStorageBytes = 0L;
        long totalSmsUsed = 0L;
        for (ShopQuotaUsage usage : usageRows) {
            totalStorageBytes += usage.getStorageBytes() == null ? 0L : usage.getStorageBytes();
            totalSmsUsed += usage.getSmsMonthUsed() == null ? 0 : usage.getSmsMonthUsed();
            Shop shop = shopById.get(usage.getShopId());
            usageItems.add(new OpsUsageItem(
                    usage.getShopId(),
                    shop == null ? ("#"+usage.getShopId()) : shop.getName(),
                    shop == null ? null : shop.getStatus(),
                    usage.getStatDate(),
                    usage.getGoodsCount(),
                    usage.getStaffCount(),
                    usage.getStorageBytes(),
                    usage.getSmsMonthUsed()
            ));
        }

        return Result.ok(new OpsOverviewResponse(
                shopTotal,
                activeShopTotal,
                pendingDomainTotal,
                orderTotal,
                platformUserTotal,
                totalStorageBytes,
                totalSmsUsed,
                LocalDateTime.now(),
                usageItems
        ));
    }

    @GetMapping("/jobs")
    public Result<List<OpsJobItem>> jobs() {
        return Result.ok(opsJobService.list().stream()
                .map(j -> new OpsJobItem(
                        j.code(), j.name(), j.cron(), j.implemented(), j.runnable(),
                        j.lastStatus(), j.lastMessage(), j.lastDurationMs(), j.lastRunAt()))
                .toList());
    }

    @PostMapping("/jobs/{code}/run")
    public Result<OpsJobRunResult> runJob(@PathVariable String code) {
        var r = opsJobService.run(code);
        return Result.ok(new OpsJobRunResult(r.code(), r.status(), r.message(), r.durationMs(), r.finishedAt()));
    }

    @GetMapping("/backups")
    public Result<List<OpsBackupItem>> backups() {
        return Result.ok(databaseBackupService.listRecent(50).stream().map(this::toBackupItem).toList());
    }

    @PostMapping("/backups")
    public Result<OpsBackupItem> createBackup(HttpServletRequest request) {
        OpsBackup row = databaseBackupService.create();
        sysLogService.record(null, "db-backup",
                "success".equals(row.getStatus()) ? "数据库备份 " + row.getFilename() : "数据库备份失败",
                ClientIp.resolve(request));
        return Result.ok(toBackupItem(row));
    }

    @GetMapping("/backups/{id}/file")
    public ResponseEntity<Resource> downloadBackup(@PathVariable Long id) {
        OpsBackup row = databaseBackupService.get(id);
        if (!"success".equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该备份没有可下载文件");
        }
        Path file = databaseBackupService.resolveFile(row);
        if (!Files.exists(file)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "备份文件不存在");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + row.getFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/sql"))
                .body(new FileSystemResource(file));
    }

    @GetMapping("/queues")
    public Result<OpsQueueOverview> queues() {
        var o = queueMetricsService.overview();
        long totalBacklog = o.queues().stream()
                .filter(QueueMetricsService.QueueStat::implemented)
                .mapToLong(q -> q.backlog() == null ? 0L : q.backlog())
                .sum();
        return Result.ok(new OpsQueueOverview(
                o.reachable(),
                o.nameServer(),
                o.message(),
                totalBacklog,
                o.queues().stream()
                        .map(q -> new OpsQueueItem(
                                q.name(), q.topic(), q.consumerGroup(), q.implemented(),
                                q.backlog(), q.deadLetter(), q.message()))
                        .toList()
        ));
    }

    @GetMapping("/cache")
    public Result<OpsCacheSnapshot> cache() {
        var snap = cacheOpsService.snapshot();
        List<OpsCacheSnapshot.ShopOption> shops = shopService.list().stream()
                .sorted(Comparator.comparing(Shop::getId))
                .map(s -> new OpsCacheSnapshot.ShopOption(s.getId(), s.getName()))
                .toList();
        return Result.ok(new OpsCacheSnapshot(
                snap.tenantKeyCount(), snap.usedMemory(), snap.maxMemory(), snap.hitRate(), shops));
    }

    @PostMapping("/cache/flush")
    public Result<OpsCacheFlushResult> flushCache(@RequestBody OpsCacheFlushRequest body, HttpServletRequest request) {
        if (body == null || body.scope() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择清除范围");
        }
        long deleted = cacheOpsService.flush(body.scope(), body.shopId());
        sysLogService.record(body.shopId(), "cache-flush",
                "清除缓存 " + body.scope() + " 删除 " + deleted + " 个 key", ClientIp.resolve(request));
        return Result.ok(new OpsCacheFlushResult(body.scope(), body.shopId(), deleted));
    }

    @GetMapping("/daily")
    public Result<List<DailyStatItem>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate statDate = date == null ? LocalDate.now().minusDays(1) : date;
        Map<Long, Shop> shops = shopService.list().stream()
                .collect(Collectors.toMap(Shop::getId, s -> s, (a, b) -> a));
        List<DailyStatItem> items = dailyReportService.listByDate(statDate).stream()
                .map(row -> toDailyItem(row, shops.get(row.getShopId())))
                .toList();
        return Result.ok(items);
    }

    private OpsBackupItem toBackupItem(OpsBackup row) {
        return new OpsBackupItem(
                row.getId(),
                row.getFilename(),
                row.getSizeBytes(),
                row.getStatus(),
                row.getMessage(),
                row.getCreateTime()
        );
    }

    private static DailyStatItem toDailyItem(StatShopDaily row, Shop shop) {
        return new DailyStatItem(
                row.getShopId(),
                shop == null ? ("#" + row.getShopId()) : shop.getName(),
                row.getStatDate(),
                row.getOrderCount(),
                row.getPayCount(),
                row.getPayAmount(),
                row.getRefundAmount(),
                row.getNewUser(),
                row.getActiveUser(),
                row.getUv(),
                row.getPv()
        );
    }
}
