package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.PrepayResponse;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pay.service.PaymentService;
import com.shopplatform.framework.security.LoginUserContext;
import com.shopplatform.framework.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.LinkedHashMap;

/**
 * 消费者端支付：列出可用渠道、发起 H5 支付并处理微信/支付宝异步通知。
 * <p>
 * 回调地址 {@code /api/pay/notify/wechat/{shopId}} 不走 {@link com.shopplatform.framework.security.ClientTenantFilter}
 * 的常规租户识别（见该类 {@code shouldNotFilter} 注释），shopId 直接来自路径，本类自行管理 TenantContext。
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    private static final Logger log = LoggerFactory.getLogger(PayController.class);

    private final PaymentService paymentService;
    private final OrderService orderService;

    @Value("${shop.pay.notify-base-url:http://localhost:8083}")
    private String notifyBaseUrl;

    @Value("${shop.pay.mock-enabled:false}")
    private boolean mockEnabled;

    @Value("${shop.pay.return-base-url:http://localhost:5175}")
    private String returnBaseUrl;

    public PayController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    @PostMapping("/{orderId}/prepay")
    public Result<PrepayResponse> prepay(@PathVariable Long orderId,
                                         @RequestParam(defaultValue = "wechat") String channel,
                                         HttpServletRequest request) {
        requireOwnOrder(orderId);
        if (mockEnabled) {
            PaymentService.PrepayResult result = paymentService.simulatePayment(channel, orderId);
            return Result.ok(new PrepayResponse(
                    result.h5Url(), result.orderNo(), result.payPrice(), true));
        }
        Long shopId = TenantContext.getRequired();
        String notifyUrl = stripTrailingSlash(notifyBaseUrl) + "/api/pay/notify/" + channel + "/" + shopId;
        String returnUrl = stripTrailingSlash(returnBaseUrl) + "/pages/order/list?_shopId=" + shopId;
        PaymentService.PrepayResult result = paymentService.createPayment(
                channel, orderId, request.getRemoteAddr(), notifyUrl, returnUrl);
        return Result.ok(new PrepayResponse(
                result.h5Url(), result.orderNo(), result.payPrice(), false));
    }

    @GetMapping("/channels")
    public Result<List<PaymentService.PayChannel>> channels() {
        List<PaymentService.PayChannel> channels = paymentService.availableChannels();
        if (mockEnabled && channels.isEmpty()) {
            channels = List.of(new PaymentService.PayChannel("wechat", "模拟支付"));
        }
        return Result.ok(channels);
    }

    private void requireOwnOrder(Long orderId) {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        Order order = orderService.getByIdWithTenant(orderId);
        if (!Objects.equals(order.getUserId(), loginUser.userId())) {
            throw new TenantAccessDeniedException("该订单不属于当前用户");
        }
    }

    /**
     * 微信支付要求的回调响应契约固定为 {@code {"code":"SUCCESS"/"FAIL","message":"..."}}，
     * 不是本项目统一的 {@link Result} 结构——这里不能借道 GlobalExceptionHandler，必须自己 try/catch
     * 拼出微信认识的失败响应，否则验签失败/处理异常时微信收到的是我们自己的错误码结构，识别不出要重试。
     */
    @PostMapping("/notify/wechat/{shopId}")
    public ResponseEntity<Map<String, String>> wechatNotify(@PathVariable Long shopId,
                                             @RequestHeader("Wechatpay-Serial") String serialNumber,
                                             @RequestHeader("Wechatpay-Nonce") String nonce,
                                             @RequestHeader("Wechatpay-Timestamp") String timestamp,
                                             @RequestHeader("Wechatpay-Signature") String signature,
                                             @RequestBody String body) {
        TenantContext.set(shopId);
        try {
            paymentService.handleWechatNotify(serialNumber, nonce, timestamp, signature, body);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (Exception e) {
            log.error("微信支付回调处理失败 shopId={}", shopId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "FAIL", "message", "处理失败"));
        } finally {
            TenantContext.clear();
        }
    }

    /** 支付宝要求成功时返回纯文本 success，否则会按其重试策略再次通知。 */
    @PostMapping(value = "/notify/alipay/{shopId}", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> alipayNotify(@PathVariable Long shopId, HttpServletRequest request) {
        TenantContext.set(shopId);
        try {
            Map<String, String> parameters = new LinkedHashMap<>();
            request.getParameterMap().forEach((key, values) ->
                    parameters.put(key, values == null ? "" : String.join(",", values)));
            paymentService.handleAlipayNotify(parameters);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("支付宝回调处理失败 shopId={}", shopId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("failure");
        } finally {
            TenantContext.clear();
        }
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
