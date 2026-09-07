package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.CheckoutRequest;
import com.shopplatform.clientapi.dto.SubmitOrderResponse;
import com.shopplatform.clientapi.service.CheckoutAppService;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.pricing.OrderPriceResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消费者端购物车结算：预览价格 / 提交订单。见文档三 §4——预览与提交必须共用同一套价格计算逻辑，
 * 两个接口内部都只经由 {@link CheckoutAppService} 走同一条 items 解析路径。
 */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutAppService checkoutAppService;

    public CheckoutController(CheckoutAppService checkoutAppService) {
        this.checkoutAppService = checkoutAppService;
    }

    @PostMapping("/preview")
    public Result<OrderPriceResult> preview(@Valid @RequestBody CheckoutRequest request) {
        return Result.ok(checkoutAppService.preview(request));
    }

    @PostMapping("/submit")
    public Result<SubmitOrderResponse> submit(@Valid @RequestBody CheckoutRequest request) {
        Order order = checkoutAppService.submit(request);
        return Result.ok(new SubmitOrderResponse(order.getId(), order.getOrderNo(), order.getPayPrice()));
    }
}
