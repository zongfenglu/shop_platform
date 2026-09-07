package com.shopplatform.domain.stats.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.stats.DailyReportService;
import com.shopplatform.domain.stats.StatGoodsDailyService;
import com.shopplatform.domain.stats.StatShopDailyService;
import com.shopplatform.domain.stats.entity.StatGoodsDaily;
import com.shopplatform.domain.stats.entity.StatShopDaily;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DailyReportServiceImpl implements DailyReportService {

    private static final Logger log = LoggerFactory.getLogger(DailyReportServiceImpl.class);

    private final ShopService shopService;
    private final OrderService orderService;
    private final OrderGoodsService orderGoodsService;
    private final AfterSaleService afterSaleService;
    private final MemberService memberService;
    private final StatShopDailyService statShopDailyService;
    private final StatGoodsDailyService statGoodsDailyService;

    public DailyReportServiceImpl(ShopService shopService,
                                  OrderService orderService,
                                  OrderGoodsService orderGoodsService,
                                  AfterSaleService afterSaleService,
                                  MemberService memberService,
                                  StatShopDailyService statShopDailyService,
                                  StatGoodsDailyService statGoodsDailyService) {
        this.shopService = shopService;
        this.orderService = orderService;
        this.orderGoodsService = orderGoodsService;
        this.afterSaleService = afterSaleService;
        this.memberService = memberService;
        this.statShopDailyService = statShopDailyService;
        this.statGoodsDailyService = statGoodsDailyService;
    }

    @Override
    public int summarizeYesterday() {
        return summarize(LocalDate.now().minusDays(1));
    }

    @Override
    public int summarize(LocalDate statDate) {
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = statDate.plusDays(1).atStartOfDay();
        int ok = 0;
        for (Long shopId : shopService.listAllShopIds()) {
            TenantContext.set(shopId);
            try {
                upsertShop(shopId, statDate, start, end);
                upsertGoods(shopId, statDate, start, end);
                ok++;
            } catch (Exception e) {
                log.error("日报汇总失败 shopId={} date={}", shopId, statDate, e);
            } finally {
                TenantContext.clear();
            }
        }
        return ok;
    }

    @Override
    public List<StatShopDaily> listByDate(LocalDate statDate) {
        return statShopDailyService.list(Wrappers.<StatShopDaily>lambdaQuery()
                .eq(StatShopDaily::getStatDate, statDate)
                .orderByDesc(StatShopDaily::getPayAmount));
    }

    @Override
    public StatShopDaily getByShop(Long shopId, LocalDate statDate) {
        return statShopDailyService.getOne(Wrappers.<StatShopDaily>lambdaQuery()
                .eq(StatShopDaily::getShopId, shopId)
                .eq(StatShopDaily::getStatDate, statDate));
    }

    private void upsertShop(Long shopId, LocalDate statDate, LocalDateTime start, LocalDateTime end) {
        List<Order> created = orderService.list(Wrappers.<Order>lambdaQuery()
                .ge(Order::getCreateTime, start)
                .lt(Order::getCreateTime, end));
        List<Order> paid = orderService.list(Wrappers.<Order>lambdaQuery()
                .eq(Order::getPayStatus, "paid")
                .ge(Order::getPayTime, start)
                .lt(Order::getPayTime, end));
        List<AfterSale> refunded = afterSaleService.list(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getStatus, "refunded")
                .ge(AfterSale::getRefundTime, start)
                .lt(AfterSale::getRefundTime, end));
        long newUser = memberService.count(Wrappers.<Member>lambdaQuery()
                .ge(Member::getCreateTime, start)
                .lt(Member::getCreateTime, end));
        long activeUser = paid.stream().map(Order::getUserId).filter(Objects::nonNull).distinct().count();
        BigDecimal payAmount = paid.stream()
                .map(o -> o.getPayPrice() == null ? BigDecimal.ZERO : o.getPayPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal refundAmount = refunded.stream()
                .map(a -> a.getRefundAmount() == null ? BigDecimal.ZERO : a.getRefundAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        StatShopDaily row = statShopDailyService.getOne(Wrappers.<StatShopDaily>lambdaQuery()
                .eq(StatShopDaily::getShopId, shopId)
                .eq(StatShopDaily::getStatDate, statDate));
        if (row == null) {
            row = new StatShopDaily();
            row.setShopId(shopId);
            row.setStatDate(statDate);
        }
        row.setOrderCount(created.size());
        row.setPayCount(paid.size());
        row.setPayAmount(payAmount);
        row.setRefundAmount(refundAmount);
        row.setNewUser((int) Math.min(Integer.MAX_VALUE, newUser));
        row.setActiveUser((int) Math.min(Integer.MAX_VALUE, activeUser));
        row.setUv(0);
        row.setPv(0);
        if (row.getId() == null) {
            statShopDailyService.save(row);
        } else {
            statShopDailyService.updateById(row);
        }
    }

    private void upsertGoods(Long shopId, LocalDate statDate, LocalDateTime start, LocalDateTime end) {
        List<Order> paid = orderService.list(Wrappers.<Order>lambdaQuery()
                .eq(Order::getPayStatus, "paid")
                .ge(Order::getPayTime, start)
                .lt(Order::getPayTime, end));
        Map<Long, Acc> byGoods = new LinkedHashMap<>();
        for (Order order : paid) {
            for (OrderGoods line : orderGoodsService.listByOrderId(order.getId())) {
                if (line.getGoodsId() == null) {
                    continue;
                }
                Acc acc = byGoods.computeIfAbsent(line.getGoodsId(), id -> new Acc());
                acc.payCount += line.getTotalNum() == null ? 0 : line.getTotalNum();
                acc.payAmount = acc.payAmount.add(line.getTotalPrice() == null ? BigDecimal.ZERO : line.getTotalPrice());
            }
        }
        for (Map.Entry<Long, Acc> e : byGoods.entrySet()) {
            StatGoodsDaily row = statGoodsDailyService.getOne(Wrappers.<StatGoodsDaily>lambdaQuery()
                    .eq(StatGoodsDaily::getShopId, shopId)
                    .eq(StatGoodsDaily::getGoodsId, e.getKey())
                    .eq(StatGoodsDaily::getStatDate, statDate));
            if (row == null) {
                row = new StatGoodsDaily();
                row.setShopId(shopId);
                row.setGoodsId(e.getKey());
                row.setStatDate(statDate);
            }
            row.setViews(0);
            row.setAddCart(0);
            row.setPayCount(e.getValue().payCount);
            row.setPayAmount(e.getValue().payAmount.setScale(2, RoundingMode.HALF_UP));
            if (row.getId() == null) {
                statGoodsDailyService.save(row);
            } else {
                statGoodsDailyService.updateById(row);
            }
        }
    }

    private static final class Acc {
        private int payCount;
        private BigDecimal payAmount = BigDecimal.ZERO;
    }
}
