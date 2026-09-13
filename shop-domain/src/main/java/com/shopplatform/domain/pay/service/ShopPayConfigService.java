package com.shopplatform.domain.pay.service;

import com.shopplatform.domain.pay.entity.ShopPayConfig;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.Optional;
import java.util.List;

public interface ShopPayConfigService extends TenantSafeService<ShopPayConfig> {

    /** 保存/覆盖商户支付配置，敏感字段在服务内部加密后落库，调用方只传明文。 */
    ShopPayConfig saveConfig(SaveConfigCommand command);

    /** 支付发起/回调时取解密后的配置，仅限支付渠道内部代码使用，绝不能经由任何 Controller 返回给前端。 */
    Optional<DecryptedPayConfig> findDecryptedConfig(String channel);

    /** 商户设置页回显用：只暴露非敏感字段 + 两个敏感字段"是否已配置"的布尔值，绝不回显明文/密文本身。 */
    Optional<MaskedPayConfig> findMasked(String channel);

    ShopPayConfig saveAlipayConfig(SaveAlipayConfigCommand command);

    Optional<DecryptedAlipayPayConfig> findDecryptedAlipayConfig();

    Optional<MaskedAlipayPayConfig> findMaskedAlipay();

    void setChannelEnabled(String channel, boolean enabled);

    List<EnabledChannel> listEnabledChannels();

    record SaveConfigCommand(
            String channel,
            String appId,
            String mchId,
            String mchCertSerialNo,
            String apiV3Key,
            String mchPrivateKeyPem
    ) {
    }

    record DecryptedPayConfig(
            Long shopId,
            String appId,
            String mchId,
            String mchCertSerialNo,
            String apiV3Key,
            String mchPrivateKeyPem
    ) {
    }

    record SaveAlipayConfigCommand(
            String appId,
            String privateKey,
            String alipayPublicKey,
            String gatewayUrl
    ) {
    }

    record DecryptedAlipayPayConfig(
            Long shopId,
            String appId,
            String privateKey,
            String alipayPublicKey,
            String gatewayUrl
    ) {
    }

    record MaskedAlipayPayConfig(
            String channel,
            String appId,
            String gatewayUrl,
            boolean privateKeySet,
            boolean alipayPublicKeySet,
            String status
    ) {
    }

    record EnabledChannel(String channel, String name) {
    }

    record MaskedPayConfig(
            String channel,
            String appId,
            String mchId,
            String mchCertSerialNo,
            boolean apiV3KeySet,
            boolean mchPrivateKeySet,
            String status
    ) {
    }
}
