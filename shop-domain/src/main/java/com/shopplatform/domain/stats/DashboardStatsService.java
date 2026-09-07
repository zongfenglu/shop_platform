package com.shopplatform.domain.stats;

import java.math.BigDecimal;
import java.util.List;

/** 店铺/平台交易看板聚合。消费者订单表带租户隔离；平台侧须 ignoreTenant。 */
public interface DashboardStatsService {

    ShopTradeOverview shopOverview();

    PlatformTradeOverview platformOverview();

    record DayGmv(String date, BigDecimal gmv, long orderCount) {
    }

    record ShopTradeOverview(
            BigDecimal todayGmv,
            long todayOrderCount,
            long unpaidCount,
            long pendingShipCount,
            long afterSaleOpenCount,
            long goodsTotal,
            long goodsOnSale,
            List<DayGmv> last7Days
    ) {
    }

    record PlatformTradeOverview(
            BigDecimal todayGmv,
            long todayOrderCount,
            long pendingShopOrders,
            BigDecimal last7DaysGmv,
            List<DayGmv> last7Days
    ) {
    }
}
