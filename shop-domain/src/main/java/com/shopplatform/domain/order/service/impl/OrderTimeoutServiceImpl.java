package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.order.service.OrderTimeoutService;
import com.shopplatform.framework.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutServiceImpl.class);

    private final OrderService orderService;
    private final int payTimeoutMinutes;

    public OrderTimeoutServiceImpl(OrderService orderService,
                                   @Value("${shop.order.pay-timeout-minutes:30}") int payTimeoutMinutes) {
        this.orderService = orderService;
        this.payTimeoutMinutes = payTimeoutMinutes;
    }

    @Override
    public int closeOverdue() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(payTimeoutMinutes);
        List<Order> overdue = TenantContext.ignoreTenant(() -> orderService.list(
                Wrappers.<Order>lambdaQuery()
                        .eq(Order::getPayStatus, "unpaid")
                        .eq(Order::getOrderStatus, "normal")
                        .lt(Order::getCreateTime, deadline)));
        int changed = 0;
        for (Order order : overdue) {
            try {
                TenantContext.set(order.getShopId());
                if (orderService.cancel(order.getId(), "超时未支付自动取消")) {
                    changed++;
                }
            } catch (Exception e) {
                log.error("超时关单失败 orderId={} shopId={}", order.getId(), order.getShopId(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return changed;
    }
}
