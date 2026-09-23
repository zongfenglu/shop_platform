package com.shopplatform.clientapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.clientapi.dto.OrderListItem;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.marketing.entity.GroupRecord;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.GroupRecordService;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderAddress;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.entity.OrderPackage;
import com.shopplatform.domain.order.logistics.LogisticsQueryService;
import com.shopplatform.domain.order.service.OrderAddressService;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderPackageService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.framework.security.LoginUserContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * 消费者端"我的订单"：列表/详情/确认收货/物流轨迹。见原型 h5/order-list.html、order-detail.html。
 * 只能查看/操作自己名下的订单——{@link #requireOwnOrder} 统一做归属校验，
 * 避免 A 用户拿到 B 用户的订单 id 就能确认收货/查看地址等信息（这属于同租户内的越权，
 * MyBatis-Plus 的 shop_id 自动过滤拦不住，必须应用层显式校验 userId）。
 */
@RestController
@RequestMapping("/api/order")
public class ConsumerOrderController {

    private final OrderService orderService;
    private final OrderGoodsService orderGoodsService;
    private final OrderAddressService orderAddressService;
    private final OrderPackageService orderPackageService;
    private final LogisticsQueryService logisticsQueryService;
    private final OfflineStoreService offlineStoreService;
    private final AfterSaleService afterSaleService;
    private final GroupActiveService groupActiveService;
    private final GroupRecordService groupRecordService;

    public ConsumerOrderController(OrderService orderService,
                                    OrderGoodsService orderGoodsService,
                                    OrderAddressService orderAddressService,
                                    OrderPackageService orderPackageService,
                                    LogisticsQueryService logisticsQueryService,
                                    OfflineStoreService offlineStoreService,
                                    AfterSaleService afterSaleService,
                                    GroupActiveService groupActiveService,
                                    GroupRecordService groupRecordService) {
        this.orderService = orderService;
        this.orderGoodsService = orderGoodsService;
        this.orderAddressService = orderAddressService;
        this.orderPackageService = orderPackageService;
        this.logisticsQueryService = logisticsQueryService;
        this.offlineStoreService = offlineStoreService;
        this.afterSaleService = afterSaleService;
        this.groupActiveService = groupActiveService;
        this.groupRecordService = groupRecordService;
    }

    @GetMapping
    public Result<IPage<OrderListItem>> list(@RequestParam(required = false) String orderStatus,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = requireLoginUserId();
        var wrapper = Wrappers.<Order>lambdaQuery().eq(Order::getUserId, userId);
        if (orderStatus != null && !orderStatus.isBlank()) {
            wrapper.eq(Order::getOrderStatus, orderStatus);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> page = new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        Page<Order> orderPage = orderService.page(page, wrapper);
        List<Long> orderIds = orderPage.getRecords().stream().map(Order::getId).toList();
        Map<Long, List<OrderGoods>> goodsByOrder = orderIds.isEmpty()
                ? Map.of()
                : orderGoodsService.list(Wrappers.<OrderGoods>lambdaQuery()
                        .in(OrderGoods::getOrderId, orderIds)
                        .orderByAsc(OrderGoods::getId))
                .stream()
                .collect(Collectors.groupingBy(OrderGoods::getOrderId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, AfterSale> latestAfterSaleByOrder = orderIds.isEmpty() ? Map.of()
                : afterSaleService.list(Wrappers.<AfterSale>lambdaQuery().in(AfterSale::getOrderId, orderIds))
                .stream().collect(Collectors.toMap(AfterSale::getOrderId, sale -> sale,
                        (left, right) -> Comparator.nullsFirst(java.time.LocalDateTime::compareTo)
                                .compare(left.getCreateTime(), right.getCreateTime()) >= 0 ? left : right));
        List<Long> groupRecordIds = orderPage.getRecords().stream()
                .map(Order::getGroupRecordId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, GroupRecord> groupRecords = groupRecordIds.isEmpty() ? Map.of()
                : groupRecordService.listByIds(groupRecordIds).stream()
                .collect(Collectors.toMap(GroupRecord::getId, r -> r));
        Map<Long, Integer> groupNums = groupRecords.isEmpty() ? Map.of()
                : groupActiveService.listByIds(groupRecords.values().stream().map(GroupRecord::getActiveId).distinct().toList())
                .stream().collect(Collectors.toMap(a -> a.getId(), a -> a.getGroupNum()));

        List<OrderListItem> records = orderPage.getRecords().stream().map(order -> {
            List<OrderGoods> goods = goodsByOrder.getOrDefault(order.getId(), List.of());
            OrderGoods first = goods.isEmpty() ? null : goods.get(0);
            int goodsCount = goods.stream().mapToInt(g -> g.getTotalNum() == null ? 0 : g.getTotalNum()).sum();
            // 已完成订单不再开放新的售后入口，避免进入申请页后没有可选商品。
            boolean deliveryAllowsAfterSale = !"shipped".equals(order.getDeliveryStatus())
                    && !"finished".equals(order.getOrderStatus());
            boolean canApplyAfterSale = deliveryAllowsAfterSale
                    && goods.stream().anyMatch(g -> "none".equals(g.getRefundStatus()));
            AfterSale latestAfterSale = latestAfterSaleByOrder.get(order.getId());
            GroupRecord group = groupRecords.get(order.getGroupRecordId());
            return new OrderListItem(
                    order.getId().toString(), order.getOrderNo(), order.getPayPrice(), order.getPayStatus(),
                    order.getDeliveryStatus(), order.getOrderStatus(), order.getCreateTime(),
                    first == null ? null : first.getGoodsName(), first == null ? null : first.getImage(),
                    first == null ? null : first.getSpecText(), goodsCount, canApplyAfterSale,
                    latestAfterSale == null ? null : latestAfterSale.getId().toString(),
                    latestAfterSale == null ? null : latestAfterSale.getStatus(),
                    order.getActivityType(),
                    order.getActivityId() == null ? null : order.getActivityId().toString(),
                    group == null ? null : group.getId().toString(),
                    group == null ? null : group.getStatus(),
                    group == null ? null : group.getActualNum(),
                    group == null ? null : groupNums.get(group.getActiveId()),
                    group == null ? null : group.getExpireTime());
        }).toList();
        Page<OrderListItem> resultPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        resultPage.setRecords(records);
        return Result.ok(resultPage);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Order order = requireOwnOrder(id);
        List<OrderGoods> goodsList = orderGoodsService.listByOrderId(id);
        OrderAddress address = orderAddressService.findByOrderId(id);
        List<OrderPackage> packages = orderPackageService.listByOrderId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("goodsList", goodsList);
        result.put("address", address == null ? Map.of() : address);
        result.put("packages", packages);
        result.put("afterSales", afterSaleService.listByOrderId(id));
        if (order.getGroupRecordId() != null) {
            GroupRecord group = groupRecordService.getByIdWithTenant(order.getGroupRecordId());
            if (group != null) {
                var active = groupActiveService.getByIdWithTenant(group.getActiveId());
                Map<String, Object> groupView = new LinkedHashMap<>();
                groupView.put("recordId", String.valueOf(group.getId()));
                groupView.put("status", group.getStatus());
                groupView.put("actualNum", group.getActualNum());
                groupView.put("groupNum", active.getGroupNum());
                groupView.put("expireTime", group.getExpireTime());
                groupView.put("successTime", group.getSuccessTime());
                groupView.put("isMock", Integer.valueOf(1).equals(active.getIsMock()));
                result.put("group", groupView);
            }
        }
        if ("pickup".equals(order.getDeliveryType()) && order.getPickupStoreId() != null) {
            result.put("pickupStore", offlineStoreService.getByIdWithTenant(order.getPickupStoreId()));
        }
        return Result.ok(result);
    }

    @PostMapping("/{id}/confirm-receipt")
    public Result<Void> confirmReceipt(@PathVariable Long id) {
        requireOwnOrder(id);
        orderService.confirmReceipt(id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        requireOwnOrder(id);
        if (!orderService.cancel(id, "用户取消")) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "只有待付款订单可以取消");
        }
        return Result.ok();
    }

    @GetMapping("/{id}/tracks")
    public Result<List<LogisticsQueryService.TrackPoint>> tracks(
            @PathVariable Long id,
            @RequestParam(required = false) Long packageId) {
        Order order = requireOwnOrder(id);
        if (!"shipped".equals(order.getDeliveryStatus()) && !"received".equals(order.getDeliveryStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单尚未发货，暂无物流信息");
        }
        List<OrderPackage> packages = orderPackageService.listByOrderId(id);
        if (packages.isEmpty()) {
            return Result.ok(List.of());
        }
        OrderPackage target = packages.get(packages.size() - 1);
        if (packageId != null) {
            target = packages.stream()
                    .filter(p -> packageId.equals(p.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "包裹不属于该订单"));
        }
        return Result.ok(logisticsQueryService.track(target.getExpressCompany(), target.getExpressNo()));
    }

    private Order requireOwnOrder(Long orderId) {
        Order order = orderService.getByIdWithTenant(orderId);
        if (!order.getUserId().equals(requireLoginUserId())) {
            // 同租户内的越权：见文档三 §2.4，必须映射为403，不能返回200+错误码。
            throw new TenantAccessDeniedException("该订单不属于当前用户");
        }
        return order;
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser.userId();
    }
}
