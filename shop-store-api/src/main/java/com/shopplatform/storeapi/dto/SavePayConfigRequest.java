package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信支付配置表单。对应原型 store/settings.html 的支付设置项——
 * apiV3Key/mchPrivateKeyPem 只在提交时经这个请求体传一次明文，服务端立即加密落库，回显接口永远不返回明文。
 */
public record SavePayConfigRequest(
        @NotBlank(message = "APPID不能为空") String appId,
        @NotBlank(message = "商户号不能为空") String mchId,
        @NotBlank(message = "证书序列号不能为空") String mchCertSerialNo,
        String apiV3Key,
        String mchPrivateKeyPem
) {
}
