package com.shopplatform.domain.order.logistics;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流轨迹查询。见开发计划 Sprint 6："物流公司查询接入（快递100/快递鸟）"。
 * <p>
 * 真实的快递100/快递鸟接入需要企业自行申请的 API Key，本地开发环境无法验证，
 * 因此本 Sprint 只落地接口契约 + 一个可本地验证的 {@link MockLogisticsQueryService} 实现；
 * 真实供应商实现类只需新增一个 {@code @Primary} 或按 {@code shop.logistics.provider} 配置切换的
 * 实现类替换掉 Mock 实现，不需要改动调用方（{@code ConsumerOrderController} 等）。
 */
public interface LogisticsQueryService {

    List<TrackPoint> track(String expressCompany, String expressNo);

    record TrackPoint(LocalDateTime time, String description, String location) {
    }
}
