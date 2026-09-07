package com.shopplatform.domain.stats;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.service.ShopOrderService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardStatsServiceImpl implements DashboardStatsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<String> OPEN_AFTER_SALE = Set.of(
            "applying", "approved", "return_shipped", "refunding");

    private final OrderService orderService;
    private final AfterSaleService afterSaleService;
    private final GoodsService goodsService;
    private final ShopOrderService shopOrderService;

    public DashboardStatsServiceImpl(OrderService orderService,
                                     AfterSaleService afterSaleService,
                                     GoodsService goodsService,
                                     ShopOrderService shopOrderService) {
        this.orderService = orderService;
        this.afterSaleService = afterSaleService;
        this.goodsService = goodsService;
        this.shopOrderService = shopOrderService;
    }

    @Override
    public ShopTradeOverview shopOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(6).atStartOfDay();
        List<Order> paid = orderService.list(Wrappers.<Order>lambdaQuery()
                .eq(Order::getPayStatus, "paid")
                .ge(Order::getPayTime, from));
        long unpaid = orderService.count(Wrappers.<Order>lambdaQuery()
                .eq(Order::getPayStatus, "unpaid")
                .eq(Order::getOrderStatus, "normal"));
        long pendingShip = orderService.count(Wrappers.<Order>lambdaQuery()
                .eq(Order::getPayStatus, "paid")
                .eq(Order::getDeliveryType, "express")
                .eq(Order::getDeliveryStatus, "pending"));
        long afterSaleOpen = afterSaleService.count(Wrappers.<AfterSale>lambdaQuery()
                .in(AfterSale::getStatus, OPEN_AFTER_SALE));
        long goodsTotal = goodsService.count();
        long goodsOnSale = goodsService.count(Wrappers.<Goods>lambdaQuery().eq(Goods::getStatus, "on"));
        List<DayGmv> last7 = bucket(paid, today);
        DayGmv todayPoint = last7.get(last7.size() - 1);
        return new ShopTradeOverview(
                todayPoint.gmv(),
                todayPoint.orderCount(),
                unpaid,
                pendingShip,
                afterSaleOpen,
                goodsTotal,
                goodsOnSale,
                last7
        );
    }

    @Override
    public PlatformTradeOverview platformOverview() {
        return TenantContext.ignoreTenant(() -> {
            LocalDate today = LocalDate.now();
            LocalDateTime from = today.minusDays(6).atStartOfDay();
            List<Order> paid = orderService.list(Wrappers.<Order>lambdaQuery()
                    .eq(Order::getPayStatus, "paid")
                    .ge(Order::getPayTime, from));
            List<DayGmv> last7 = bucket(paid, today);
            DayGmv todayPoint = last7.get(last7.size() - 1);
            BigDecimal week = last7.stream().map(DayGmv::gmv).reduce(BigDecimal.ZERO, BigDecimal::add);
            long pendingShopOrders = shopOrderService.count(
                    Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getPayStatus, "pending"));
            return new PlatformTradeOverview(
                    todayPoint.gmv(),
                    todayPoint.orderCount(),
                    pendingShopOrders,
                    week,
                    last7
            );
        });
    }

    private static List<DayGmv> bucket(List<Order> paid, LocalDate today) {
        Map<String, List<Order>> byDay = paid.stream()
                .filter(o -> o.getPayTime() != null)
                .collect(Collectors.groupingBy(o -> o.getPayTime().toLocalDate().format(DAY)));
        List<DayGmv> out = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String key = d.format(DAY);
            List<Order> rows = byDay.getOrDefault(key, List.of());
            BigDecimal gmv = rows.stream()
                    .map(o -> o.getPayPrice() == null ? BigDecimal.ZERO : o.getPayPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            out.add(new DayGmv(key, gmv, rows.size()));
        }
        return out;
    }
}
