package com.shopplatform.domain.ops.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.aftersale.service.AfterSaleTimeoutService;
import com.shopplatform.domain.ops.OpsJobService;
import com.shopplatform.domain.order.service.OrderAutoConfirmService;
import com.shopplatform.domain.order.service.OrderTimeoutService;
import com.shopplatform.domain.shop.entity.ShopQuotaUsage;
import com.shopplatform.domain.shop.service.ShopExpireRemindService;
import com.shopplatform.domain.shop.service.ShopExpireService;
import com.shopplatform.domain.shop.service.ShopQuotaSnapshotService;
import com.shopplatform.domain.shop.service.ShopQuotaUsageService;
import com.shopplatform.domain.mp.MpComponentService;
import com.shopplatform.domain.ssl.SslCertificateService;
import com.shopplatform.domain.stats.DailyReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OpsJobServiceImpl implements OpsJobService {

    private static final List<Def> CATALOG = List.of(
            new Def("order-timeout", "订单超时关闭", "每5分钟（主链路延时消息）", true, true),
            new Def("auto-confirm", "自动确认收货", "每小时", true, true),
            new Def("after-sale-timeout", "售后超时自动同意", "每小时", true, true),
            new Def("dealer-settle", "分销佣金结算", "每小时", true, false),
            new Def("coupon-expire", "优惠券过期", "每天 00:10", true, false),
            new Def("member-grade", "会员等级升级", "每天 00:30", true, false),
            new Def("seckill-stock", "秒杀库存对账", "每分钟", true, false),
            new Def("group-expire", "拼团超时未成团", "每分钟", true, false),
            new Def("bargain-expire", "砍价活动过期", "每小时", true, false),
            new Def("shop-expire", "租户到期检查", "每天 00:05", true, true),
            new Def("shop-expire-remind", "租户到期提醒", "每天 09:00", true, true),
            new Def("quota-snapshot", "配额用量统计", "每天 01:00", true, true),
            new Def("daily-report", "日报汇总", "每天 01:30", true, true),
            new Def("mp-publish", "小程序发布任务调度", "每分钟", false, false),
            new Def("wechat-ticket", "微信票据刷新", "每10分钟", true, true),
            new Def("cert-renew", "证书续期", "每天 03:00", true, true),
            new Def("pay-compensate", "支付状态补偿查询", "每5分钟", false, false),
            new Def("logistics-sync", "物流轨迹同步", "每小时", false, false)
    );

    private final ShopQuotaSnapshotService shopQuotaSnapshotService;
    private final ShopExpireService shopExpireService;
    private final ShopQuotaUsageService shopQuotaUsageService;
    private final OrderAutoConfirmService orderAutoConfirmService;
    private final AfterSaleTimeoutService afterSaleTimeoutService;
    private final OrderTimeoutService orderTimeoutService;
    private final DailyReportService dailyReportService;
    private final ShopExpireRemindService shopExpireRemindService;
    private final MpComponentService mpComponentService;
    private final SslCertificateService sslCertificateService;
    private final Map<String, JobRunResult> lastRuns = new ConcurrentHashMap<>();

    public OpsJobServiceImpl(ShopQuotaSnapshotService shopQuotaSnapshotService,
                             ShopExpireService shopExpireService,
                             ShopQuotaUsageService shopQuotaUsageService,
                             OrderAutoConfirmService orderAutoConfirmService,
                             AfterSaleTimeoutService afterSaleTimeoutService,
                             OrderTimeoutService orderTimeoutService,
                             DailyReportService dailyReportService,
                             ShopExpireRemindService shopExpireRemindService,
                             MpComponentService mpComponentService,
                             SslCertificateService sslCertificateService) {
        this.shopQuotaSnapshotService = shopQuotaSnapshotService;
        this.shopExpireService = shopExpireService;
        this.shopQuotaUsageService = shopQuotaUsageService;
        this.orderAutoConfirmService = orderAutoConfirmService;
        this.afterSaleTimeoutService = afterSaleTimeoutService;
        this.orderTimeoutService = orderTimeoutService;
        this.dailyReportService = dailyReportService;
        this.shopExpireRemindService = shopExpireRemindService;
        this.mpComponentService = mpComponentService;
        this.sslCertificateService = sslCertificateService;
    }

    @Override
    public List<JobInfo> list() {
        List<JobInfo> out = new ArrayList<>();
        for (Def def : CATALOG) {
            JobRunResult last = lastRuns.get(def.code);
            if (last == null && "quota-snapshot".equals(def.code)) {
                last = inferQuotaLastRun();
            }
            out.add(new JobInfo(
                    def.code,
                    def.name,
                    def.cron,
                    def.implemented,
                    def.runnable,
                    last == null ? (def.implemented ? "待执行" : "未实现") : last.status(),
                    last == null ? (def.implemented ? "由 shop-job 定时调度" : "按排期后续接入") : last.message(),
                    last == null ? null : last.durationMs(),
                    last == null ? null : last.finishedAt()
            ));
        }
        return out;
    }

    @Override
    public JobRunResult run(String code) {
        if (!List.of(
                "quota-snapshot", "shop-expire", "shop-expire-remind",
                "auto-confirm", "after-sale-timeout", "order-timeout", "daily-report", "wechat-ticket", "cert-renew"
        ).contains(code)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该任务暂不支持手动执行");
        }
        long start = System.currentTimeMillis();
        String message;
        String status = "成功";
        try {
            message = switch (code) {
                case "quota-snapshot" -> "已写入 " + shopQuotaSnapshotService.snapshotAll() + " 家商城今日用量";
                case "shop-expire" -> "已将 " + shopExpireService.expireDue() + " 家到期商城置为过期";
                case "shop-expire-remind" -> "已写入 " + shopExpireRemindService.remindDue() + " 条到期提醒";
                case "auto-confirm" -> "已自动确认 " + orderAutoConfirmService.confirmOverdue() + " 笔超时未收货订单";
                case "after-sale-timeout" -> "已自动同意 " + afterSaleTimeoutService.approveOverdue() + " 笔超时售后";
                case "order-timeout" -> "已关闭 " + orderTimeoutService.closeOverdue() + " 笔超时未支付订单";
                case "wechat-ticket" -> {
                    var token = mpComponentService.refreshComponentToken();
                    yield "component_access_token 已刷新，有效期 " + token.expiresIn() + " 秒";
                }
                case "cert-renew" -> "已续期/重试 " + sslCertificateService.renewDue() + " 张证书";
                case "daily-report" -> "已汇总 " + dailyReportService.summarizeYesterday() + " 家商城昨日日报";
                default -> throw new BusinessException(ErrorCode.PARAM_INVALID, "该任务暂不支持手动执行");
            };
        } catch (RuntimeException e) {
            status = "失败";
            message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            JobRunResult fail = new JobRunResult(code, status, message, System.currentTimeMillis() - start, LocalDateTime.now());
            lastRuns.put(code, fail);
            throw e;
        }
        JobRunResult result = new JobRunResult(code, status, message, System.currentTimeMillis() - start, LocalDateTime.now());
        lastRuns.put(code, result);
        return result;
    }

    private JobRunResult inferQuotaLastRun() {
        List<ShopQuotaUsage> latest = shopQuotaUsageService.listLatestSnapshots(1);
        if (latest.isEmpty() || latest.get(0).getStatDate() == null) {
            return null;
        }
        return new JobRunResult(
                "quota-snapshot",
                "成功",
                "最近快照日 " + latest.get(0).getStatDate(),
                0L,
                latest.get(0).getCreateTime()
        );
    }

    private record Def(String code, String name, String cron, boolean implemented, boolean runnable) {
    }
}
