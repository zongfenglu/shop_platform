package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.PrepayResponse;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.pay.service.PaymentService;
import com.shopplatform.framework.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消费者端支付：发起支付（H5支付，返回微信 h5_url 供跳转）+ 支付回调。
 * <p>
 * 回调地址 {@code /api/pay/notify/wechat/{shopId}} 不走 {@link com.shopplatform.framework.security.ClientTenantFilter}
 * 的常规租户识别（见该类 {@code shouldNotFilter} 注释），shopId 直接来自路径，本类自行管理 TenantContext。
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    private static final Logger log = LoggerFactory.getLogger(PayController.class);

    private final PaymentService paymentService;

    @Value("${shop.pay.notify-base-url:http://localhost:8083}")
    private String notifyBaseUrl;

    public PayController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}/prepay")
    public Result<PrepayResponse> prepay(@PathVariable Long orderId, HttpServletRequest request) {
        Long shopId = TenantContext.getRequired();
        String notifyUrl = notifyBaseUrl + "/api/pay/notify/wechat/" + shopId;
        PaymentService.PrepayResult result = paymentService.createPayment(
                orderId, request.getRemoteAddr(), notifyUrl);
        return Result.ok(new PrepayResponse(result.h5Url(), result.orderNo(), result.payPrice()));
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
}
