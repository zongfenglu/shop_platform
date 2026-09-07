package com.shopplatform.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.adminapi.dto.ShopOrderItem;
import com.shopplatform.adminapi.dto.ShopOrderListQuery;
import com.shopplatform.adminapi.dto.ShopOrderPageResponse;
import com.shopplatform.adminapi.dto.ShopOrderSummary;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.shop.entity.PackageTpl;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.service.PackageTplService;
import com.shopplatform.domain.shop.service.ShopOrderService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.service.ShopSubscriptionService;
import com.shopplatform.framework.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台订单管理。对应原型 admin/order-list.html。
 */
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final ShopOrderService shopOrderService;
    private final ShopService shopService;
    private final PackageTplService packageTplService;
    private final ShopSubscriptionService shopSubscriptionService;
    private final SysLogService sysLogService;

    public AdminOrderController(ShopOrderService shopOrderService,
                                ShopService shopService,
                                PackageTplService packageTplService,
                                ShopSubscriptionService shopSubscriptionService,
                                SysLogService sysLogService) {
        this.shopOrderService = shopOrderService;
        this.shopService = shopService;
        this.packageTplService = packageTplService;
        this.shopSubscriptionService = shopSubscriptionService;
        this.sysLogService = sysLogService;
    }

    @GetMapping
    public Result<ShopOrderPageResponse> page(ShopOrderListQuery query) {
        var wrapper = Wrappers.<ShopOrder>lambdaQuery();

        String keyword = StringUtils.hasText(query.keyword()) ? query.keyword().trim() : null;
        if (StringUtils.hasText(keyword)) {
            List<Long> shopIds = shopService.list(
                    Wrappers.<Shop>lambdaQuery()
                            .and(w -> w.like(Shop::getName, keyword)
                                    .or().like(Shop::getCode, keyword)
                                    .or().like(Shop::getMobile, keyword)))
                    .stream()
                    .map(Shop::getId)
                    .toList();
            wrapper.and(w -> {
                w.like(ShopOrder::getOrderNo, keyword);
                if (!shopIds.isEmpty()) {
                    w.or().in(ShopOrder::getShopId, shopIds);
                }
            });
        }

        applyTypeFilter(wrapper, query.type());
        wrapper.orderByDesc(ShopOrder::getCreateTime);

        Page<ShopOrder> page = shopOrderService.page(
                new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault()), wrapper);

        List<ShopOrderItem> records = toItems(page.getRecords());
        ShopOrderSummary summary = buildSummary();
        return Result.ok(new ShopOrderPageResponse(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), summary));
    }

    @PostMapping("/{id}/confirm-paid")
    public Result<ShopOrderItem> confirmPaid(@PathVariable Long id, HttpServletRequest request) {
        ShopOrder order = shopSubscriptionService.confirmPaid(id);
        sysLogService.record(order.getShopId(), "confirm-paid", "确认到账 " + order.getOrderNo(), ClientIp.resolve(request));
        return Result.ok(toItems(List.of(order)).get(0));
    }

    private void applyTypeFilter(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ShopOrder> wrapper, String type) {
        if (!StringUtils.hasText(type) || "all".equals(type)) {
            return;
        }
        if ("refund".equals(type)) {
            wrapper.eq(ShopOrder::getPayStatus, "refunded");
            return;
        }
        if ("service".equals(type)) {
            wrapper.eq(ShopOrder::getType, "addon");
            return;
        }
        wrapper.eq(ShopOrder::getType, type.trim());
    }

    private ShopOrderSummary buildSummary() {
        long total = shopOrderService.count();
        long pending = shopOrderService.count(Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getPayStatus, "pending"));
        long paid = shopOrderService.count(Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getPayStatus, "paid"));
        long refunded = shopOrderService.count(Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getPayStatus, "refunded"));
        long newCount = shopOrderService.count(Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getType, "new"));
        long renewCount = shopOrderService.count(Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getType, "renew"));
        long upgradeCount = shopOrderService.count(Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getType, "upgrade"));
        long addonCount = shopOrderService.count(Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getType, "addon"));
        return new ShopOrderSummary(total, pending, paid, refunded, newCount, renewCount, upgradeCount, addonCount);
    }

    private List<ShopOrderItem> toItems(List<ShopOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        Map<Long, Shop> shopMap = shopService.listByIds(orders.stream().map(ShopOrder::getShopId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Shop::getId, item -> item));
        Map<Long, PackageTpl> tplMap = packageTplService.listByIds(
                        orders.stream()
                                .map(ShopOrder::getPackageTplId)
                                .filter(id -> id != null && id > 0)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(PackageTpl::getId, item -> item));

        return orders.stream()
                .map(order -> new ShopOrderItem(
                        order.getId(),
                        order.getOrderNo(),
                        order.getShopId(),
                        shopMap.get(order.getShopId()) == null ? "未知商家" : shopMap.get(order.getShopId()).getName(),
                        order.getType(),
                        order.getPackageTplId(),
                        tplMap.get(order.getPackageTplId()) == null ? null : tplMap.get(order.getPackageTplId()).getName(),
                        order.getDurationMonth(),
                        order.getAmount(),
                        order.getPayStatus(),
                        order.getPayMethod(),
                        order.getPayTime(),
                        order.getCreateTime()
                ))
                .toList();
    }
}
