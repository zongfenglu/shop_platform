package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import com.shopplatform.domain.pay.service.ShopPayConfigService.MaskedPayConfig;
import com.shopplatform.storeapi.dto.SavePayConfigRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 商户支付设置。对应原型 store/settings.html 支付设置项。
 * 敏感字段（APIv3密钥/商户私钥）只进不出：{@link #save} 接收明文并立即加密落库，
 * {@link #get} 只回显掩码信息，任何时候都不会把明文/密文吐给前端。
 */
@RestController
@RequestMapping("/store/pay-config")
public class StorePayConfigController {

    private static final String CHANNEL = "wechat";

    private final ShopPayConfigService shopPayConfigService;

    public StorePayConfigController(ShopPayConfigService shopPayConfigService) {
        this.shopPayConfigService = shopPayConfigService;
    }

    @GetMapping
    public Result<MaskedPayConfig> get() {
        return Result.ok(shopPayConfigService.findMasked(CHANNEL).orElse(null));
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody SavePayConfigRequest request) {
        shopPayConfigService.saveConfig(new ShopPayConfigService.SaveConfigCommand(
                CHANNEL, request.appId(), request.mchId(), request.mchCertSerialNo(),
                request.apiV3Key(), request.mchPrivateKeyPem()));
        return Result.ok();
    }
}
