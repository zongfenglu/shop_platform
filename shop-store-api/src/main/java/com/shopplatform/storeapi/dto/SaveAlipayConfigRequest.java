package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotBlank;

/** 支付宝 WAP 支付配置。两个密钥字段更新时可留空以保留已保存值。 */
public record SaveAlipayConfigRequest(
        @NotBlank(message = "支付宝应用ID不能为空") String appId,
        String privateKey,
        String alipayPublicKey,
        String gatewayUrl
) {
}
