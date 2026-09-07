package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.ApplyAfterSaleRequest;
import com.shopplatform.clientapi.dto.ReturnShippedRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.framework.security.LoginUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    public ConsumerAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping
    public Result<AfterSale> apply(@Valid @RequestBody ApplyAfterSaleRequest request) {
        Long userId = requireLoginUserId();
        AfterSale afterSale = afterSaleService.apply(new AfterSaleService.ApplyCommand(
                request.orderId(), request.orderGoodsId(), userId, request.type(),
                request.applyReason(), request.applyDesc(), request.images(), request.refundNum()));
        return Result.ok(afterSale);
    }

    @GetMapping("/{id}")
    public Result<AfterSale> detail(@PathVariable Long id) {
        return Result.ok(requireOwnAfterSale(id));
    }

    @GetMapping("/order/{orderId}")
    public Result<List<AfterSale>> listByOrder(@PathVariable Long orderId) {
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
