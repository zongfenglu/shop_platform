package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderAddress;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.entity.OrderPackage;
import com.shopplatform.domain.order.service.OrderAddressService;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderPackageService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.setting.service.ExpressCompanyService;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.storeapi.dto.OrderListQuery;
import com.shopplatform.storeapi.dto.ShipOrderRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商户端订单列表/详情/发货/关单。对应原型 store/order-list.html、order-detail.html。
 */
@RestController
@RequestMapping("/store/order")
public class StoreOrderController {

    private final OrderService orderService;
    private final OrderGoodsService orderGoodsService;
    private final OrderAddressService orderAddressService;
    private final OrderPackageService orderPackageService;
    private final ExpressCompanyService expressCompanyService;

    public StoreOrderController(OrderService orderService,
                                 OrderGoodsService orderGoodsService,
                                 OrderAddressService orderAddressService,
                                 OrderPackageService orderPackageService,
                                 ExpressCompanyService expressCompanyService) {
        this.orderService = orderService;
        this.orderGoodsService = orderGoodsService;
        this.orderAddressService = orderAddressService;
        this.orderPackageService = orderPackageService;
        this.expressCompanyService = expressCompanyService;
    }

    @GetMapping
    public Result<IPage<Order>> list(OrderListQuery query) {
        var wrapper = Wrappers.<Order>lambdaQuery();
        if (StringUtils.hasText(query.orderNo())) {
            wrapper.eq(Order::getOrderNo, query.orderNo());
        }
        if (StringUtils.hasText(query.payStatus())) {
            wrapper.eq(Order::getPayStatus, query.payStatus());
        }
        if (StringUtils.hasText(query.deliveryStatus())) {
            wrapper.eq(Order::getDeliveryStatus, query.deliveryStatus());
        }
        if (StringUtils.hasText(query.orderStatus())) {
            wrapper.eq(Order::getOrderStatus, query.orderStatus());
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault());
        return Result.ok(orderService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Order order = orderService.getByIdWithTenant(id);
        List<OrderGoods> goodsList = orderGoodsService.listByOrderId(id);
        OrderAddress address = orderAddressService.findByOrderId(id);
        List<OrderPackage> packages = orderPackageService.listByOrderId(id);
        return Result.ok(Map.of("order", order, "goodsList", goodsList,
                "address", address == null ? Map.of() : address, "packages", packages));
    }

    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @Valid @RequestBody ShipOrderRequest request) {
        boolean supported = expressCompanyService.listEnabled().stream()
                .anyMatch(company -> company.getName().equals(request.expressCompany()));
        if (!supported) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择已启用的物流公司");
        }
        orderService.ship(id, new OrderService.ShipCommand(
                request.expressCompany(), request.expressNo(), request.orderGoodsIds()));
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        orderService.cancel(id, reason == null ? "商户关闭订单" : reason);
        return Result.ok();
    }
}
