package com.shopplatform.clientapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.clientapi.dto.ApplyAfterSaleRequest;
import com.shopplatform.clientapi.dto.AfterSaleListItem;
import com.shopplatform.clientapi.dto.ReturnShippedRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.framework.security.LoginUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消费者端申请售后。见原型 h5/order-list.html 的"申请售后"入口。
 * 归属校验交给 {@link AfterSaleService#apply}（内部校验 order.userId），
 * 但 return-shipped/close 这两个后续动作发生在售后单已存在之后，这里需要再校验一次售后单本身的 userId，
 * 避免 A 用户拿到 B 用户的售后单 id 就能操作（同租户内越权，MyBatis-Plus 的 shop_id 过滤拦不住）。
 */
@RestController
@RequestMapping("/api/after-sale")
public class ConsumerAfterSaleController {

    private final AfterSaleService afterSaleService;
    private final OrderService orderService;
    private final OrderGoodsService orderGoodsService;

    public ConsumerAfterSaleController(AfterSaleService afterSaleService,
                                       OrderService orderService,
                                       OrderGoodsService orderGoodsService) {
        this.afterSaleService = afterSaleService;
        this.orderService = orderService;
        this.orderGoodsService = orderGoodsService;
    }

    @PostMapping
    public Result<AfterSale> apply(@Valid @RequestBody ApplyAfterSaleRequest request) {
        Long userId = requireLoginUserId();
        AfterSale afterSale = afterSaleService.apply(new AfterSaleService.ApplyCommand(
                request.orderId(), request.orderGoodsId(), userId, request.type(),
                request.applyReason(), request.applyDesc(), request.images(), request.refundNum()));
        return Result.ok(afterSale);
    }

    /**
     * 买家的售后记录。scope=processing 查询处理中，scope=review 查询已退款待评价，
     * scope=records 或不传查询全部申请记录。
     */
    @GetMapping
    public Result<IPage<AfterSaleListItem>> list(@RequestParam(required = false) String scope,
                                                 @RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = requireLoginUserId();
        var wrapper = Wrappers.<AfterSale>lambdaQuery().eq(AfterSale::getUserId, userId);
        if ("processing".equals(scope)) {
            wrapper.in(AfterSale::getStatus, List.of("applying", "approved", "return_shipped", "refunding"));
        } else if ("review".equals(scope)) {
            wrapper.eq(AfterSale::getStatus, "refunded");
        }
        wrapper.orderByDesc(AfterSale::getCreateTime);

        Page<AfterSale> source = afterSaleService.page(
                new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)), wrapper);
        List<Long> orderIds = source.getRecords().stream().map(AfterSale::getOrderId).distinct().toList();
        List<Long> orderGoodsIds = source.getRecords().stream().map(AfterSale::getOrderGoodsId).distinct().toList();
        Map<Long, Order> orders = orderIds.isEmpty() ? Map.of() : orderService.list(
                        Wrappers.<Order>lambdaQuery().in(Order::getId, orderIds))
                .stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        Map<Long, OrderGoods> goods = orderGoodsIds.isEmpty() ? Map.of() : orderGoodsService.list(
                        Wrappers.<OrderGoods>lambdaQuery().in(OrderGoods::getId, orderGoodsIds))
                .stream().collect(Collectors.toMap(OrderGoods::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<AfterSaleListItem> records = source.getRecords().stream().map(row -> {
            Order order = orders.get(row.getOrderId());
            OrderGoods item = goods.get(row.getOrderGoodsId());
            return new AfterSaleListItem(
                    row.getId(), row.getOrderId(), row.getOrderGoodsId(), order == null ? null : order.getOrderNo(),
                    item == null ? null : item.getGoodsName(), item == null ? null : item.getImage(),
                    item == null ? null : item.getSpecText(), row.getRefundNum(), row.getType(),
                    row.getApplyReason(), row.getRefundAmount(), row.getStatus(), row.getCreateTime());
        }).toList();
        Page<AfterSaleListItem> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(records);
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<AfterSale> detail(@PathVariable Long id) {
        return Result.ok(requireOwnAfterSale(id));
    }

    @GetMapping("/order/{orderId}")
    public Result<List<AfterSale>> listByOrder(@PathVariable Long orderId) {
        Order order = orderService.getByIdWithTenant(orderId);
        if (!order.getUserId().equals(requireLoginUserId())) {
            throw new TenantAccessDeniedException("该订单不属于当前用户");
        }
        return Result.ok(afterSaleService.listByOrderId(orderId));
    }

    @PostMapping("/{id}/return-shipped")
    public Result<Void> returnShipped(@PathVariable Long id, @Valid @RequestBody ReturnShippedRequest request) {
        requireOwnAfterSale(id);
        afterSaleService.confirmReturnShipped(id, request.expressCompany(), request.expressNo());
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        requireOwnAfterSale(id);
        afterSaleService.close(id);
        return Result.ok();
    }

    private AfterSale requireOwnAfterSale(Long id) {
        AfterSale afterSale = afterSaleService.getByIdWithTenant(id);
        if (!afterSale.getUserId().equals(requireLoginUserId())) {
            // 同租户内的越权：见文档三 §2.4，必须映射为403，不能返回200+错误码。
            throw new TenantAccessDeniedException("该售后单不属于当前用户");
        }
        return afterSale;
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser.userId();
    }
}
