package com.shopplatform.domain.order.logistics;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地开发用 Mock 实现：按快递单号生成一组确定性的模拟轨迹，不依赖任何外部网络请求，
 * 可在没有真实快递100/快递鸟 API Key 的环境下验证"物流轨迹接口->订单详情联动"这条链路是否打通。
 * 生产环境应替换为真实供应商实现（见 {@link LogisticsQueryService} 类注释）。
 */
@Component
public class MockLogisticsQueryService implements LogisticsQueryService {

    @Override
    public List<TrackPoint> track(String expressCompany, String expressNo) {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new TrackPoint(now.minusHours(1), "快件已被签收，感谢使用" + expressCompany, "收件地"),
                new TrackPoint(now.minusHours(6), "快件正在派送途中", "本地网点"),
                new TrackPoint(now.minusHours(18), "快件已到达分拨中心", "中转城市"),
                new TrackPoint(now.minusHours(30), "商家已发货，单号" + expressNo, "发件地")
        );
    }
}
