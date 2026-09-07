package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderPackage;
import com.shopplatform.domain.order.service.OrderAutoConfirmService;
import com.shopplatform.domain.order.service.OrderPackageService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderAutoConfirmServiceImpl implements OrderAutoConfirmService {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoConfirmServiceImpl.class);

    private final OrderService orderService;
    private final OrderPackageService orderPackageService;
    private final int autoConfirmDays;

    public OrderAutoConfirmServiceImpl(OrderService orderService,
                                       OrderPackageService orderPackageService,
                                       @Value("${shop.trade.auto-confirm-days:15}") int autoConfirmDays) {
        this.orderService = orderService;
        this.orderPackageService = orderPackageService;
        this.autoConfirmDays = autoConfirmDays;
    }

    @Override
    public int confirmOverdue() {
        List<Order> shipped = TenantContext.ignoreTenant(() -> orderService.list(Wrappers.<Order>lambdaQuery()
                .eq(Order::getDeliveryType, "express")
                .eq(Order::getDeliveryStatus, "shipped")));
        LocalDateTime now = LocalDateTime.now();
        int changed = 0;
        for (Order order : shipped) {
            try {
                TenantContext.set(order.getShopId());
                List<OrderPackage> packages = orderPackageService.listByOrderId(order.getId());
                OrderPackage first = packages.stream()
                        .filter(p -> p.getCreateTime() != null)
                        .min(Comparator.comparing(OrderPackage::getCreateTime))
                        .orElse(null);
                if (first == null || !first.getCreateTime().plusDays(autoConfirmDays).isBefore(now)) {
                    continue;
                }
                orderService.confirmReceipt(order.getId());
                changed++;
            } catch (Exception e) {
                log.error("自动确认收货失败 orderId={} shopId={}", order.getId(), order.getShopId(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return changed;
    }
}
