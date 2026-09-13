package com.shopplatform.domain.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.pay.entity.ShopPayConfig;
import com.shopplatform.domain.pay.mapper.ShopPayConfigMapper;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.List;
import java.net.URI;
import java.util.Set;

@Service
public class ShopPayConfigServiceImpl extends ServiceImpl<ShopPayConfigMapper, ShopPayConfig>
        implements ShopPayConfigService {

    private static final String DEFAULT_ALIPAY_GATEWAY = "https://openapi.alipay.com/gateway.do";
    private static final Set<String> ALIPAY_GATEWAY_HOSTS = Set.of(
            "openapi.alipay.com", "openapi-sandbox.dl.alipaydev.com");

    private final AesGcmEncryptor aesGcmEncryptor;

    public ShopPayConfigServiceImpl(AesGcmEncryptor aesGcmEncryptor) {
        this.aesGcmEncryptor = aesGcmEncryptor;
    }

    @Override
    public ShopPayConfig saveConfig(SaveConfigCommand cmd) {
        ShopPayConfig existing = this.getOne(
                Wrappers.<ShopPayConfig>lambdaQuery().eq(ShopPayConfig::getChannel, cmd.channel()));

        ShopPayConfig config = existing == null ? new ShopPayConfig() : existing;
        config.setChannel(cmd.channel());
        config.setAppId(cmd.appId());
        config.setMchId(cmd.mchId());
        config.setMchCertSerialNo(cmd.mchCertSerialNo());
        config.setApiV3KeyEncrypted(encryptOrKeep(cmd.apiV3Key(), config.getApiV3KeyEncrypted(), "APIv3密钥"));
        config.setMchPrivateKeyEncrypted(encryptOrKeep(
                cmd.mchPrivateKeyPem(), config.getMchPrivateKeyEncrypted(), "商户私钥"));
        config.setStatus("enabled");
        config.setSortNo(config.getSortNo() == null ? 10 : config.getSortNo());
        this.saveOrUpdate(config);
        return config;
    }

    @Override
    public Optional<DecryptedPayConfig> findDecryptedConfig(String channel) {
        ShopPayConfig config = this.getOne(Wrappers.<ShopPayConfig>lambdaQuery()
                .eq(ShopPayConfig::getChannel, channel)
                .eq(ShopPayConfig::getStatus, "enabled"));
        if (config == null) {
            return Optional.empty();
        }
        return Optional.of(new DecryptedPayConfig(
                config.getShopId(), config.getAppId(), config.getMchId(), config.getMchCertSerialNo(),
                aesGcmEncryptor.decrypt(config.getApiV3KeyEncrypted()),
                aesGcmEncryptor.decrypt(config.getMchPrivateKeyEncrypted())));
    }

    @Override
    public Optional<MaskedPayConfig> findMasked(String channel) {
        ShopPayConfig config = this.getOne(Wrappers.<ShopPayConfig>lambdaQuery().eq(ShopPayConfig::getChannel, channel));
        if (config == null) {
            return Optional.empty();
        }
        return Optional.of(new MaskedPayConfig(
                config.getChannel(), config.getAppId(), config.getMchId(), config.getMchCertSerialNo(),
                StringUtils.hasText(config.getApiV3KeyEncrypted()),
                StringUtils.hasText(config.getMchPrivateKeyEncrypted()),
                config.getStatus()));
    }

    @Override
    public ShopPayConfig saveAlipayConfig(SaveAlipayConfigCommand cmd) {
        ShopPayConfig existing = this.getOne(
                Wrappers.<ShopPayConfig>lambdaQuery().eq(ShopPayConfig::getChannel, "alipay"));
        ShopPayConfig config = existing == null ? new ShopPayConfig() : existing;
        config.setChannel("alipay");
        config.setAppId(cmd.appId());
        config.setMchPrivateKeyEncrypted(encryptOrKeep(
                cmd.privateKey(), config.getMchPrivateKeyEncrypted(), "支付宝应用私钥"));
        config.setAlipayPublicKeyEncrypted(encryptOrKeep(
                cmd.alipayPublicKey(), config.getAlipayPublicKeyEncrypted(), "支付宝公钥"));
        config.setGatewayUrl(normalizeAlipayGateway(cmd.gatewayUrl()));
        config.setStatus("enabled");
        config.setSortNo(config.getSortNo() == null ? 20 : config.getSortNo());
        this.saveOrUpdate(config);
        return config;
    }

    @Override
    public Optional<DecryptedAlipayPayConfig> findDecryptedAlipayConfig() {
        ShopPayConfig config = enabledConfig("alipay");
        if (config == null) {
            return Optional.empty();
        }
        return Optional.of(new DecryptedAlipayPayConfig(
                config.getShopId(), config.getAppId(),
                aesGcmEncryptor.decrypt(config.getMchPrivateKeyEncrypted()),
                aesGcmEncryptor.decrypt(config.getAlipayPublicKeyEncrypted()), config.getGatewayUrl()));
    }

    @Override
    public Optional<MaskedAlipayPayConfig> findMaskedAlipay() {
        ShopPayConfig config = channelConfig("alipay");
        if (config == null) {
            return Optional.empty();
        }
        return Optional.of(new MaskedAlipayPayConfig(
                config.getChannel(), config.getAppId(), config.getGatewayUrl(),
                StringUtils.hasText(config.getMchPrivateKeyEncrypted()),
                StringUtils.hasText(config.getAlipayPublicKeyEncrypted()), config.getStatus()));
    }

    @Override
    public void setChannelEnabled(String channel, boolean enabled) {
        if (!"wechat".equals(channel) && !"alipay".equals(channel)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的支付渠道");
        }
        ShopPayConfig config = channelConfig(channel);
        if (config == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请先完成渠道配置");
        }
        if (enabled && !isComplete(config)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "支付渠道配置不完整，无法启用");
        }
        config.setStatus(enabled ? "enabled" : "disabled");
        this.updateById(config);
    }

    @Override
    public List<EnabledChannel> listEnabledChannels() {
        return this.list(Wrappers.<ShopPayConfig>lambdaQuery()
                        .eq(ShopPayConfig::getStatus, "enabled")
                        .in(ShopPayConfig::getChannel, "wechat", "alipay")
                        .orderByAsc(ShopPayConfig::getSortNo))
                .stream()
                .filter(this::isComplete)
                .map(config -> new EnabledChannel(config.getChannel(),
                        "wechat".equals(config.getChannel()) ? "微信支付" : "支付宝"))
                .toList();
    }

    private ShopPayConfig channelConfig(String channel) {
        return this.getOne(Wrappers.<ShopPayConfig>lambdaQuery().eq(ShopPayConfig::getChannel, channel));
    }

    private ShopPayConfig enabledConfig(String channel) {
        return this.getOne(Wrappers.<ShopPayConfig>lambdaQuery()
                .eq(ShopPayConfig::getChannel, channel).eq(ShopPayConfig::getStatus, "enabled"));
    }

    private boolean isComplete(ShopPayConfig config) {
        if ("wechat".equals(config.getChannel())) {
            return StringUtils.hasText(config.getAppId()) && StringUtils.hasText(config.getMchId())
                    && StringUtils.hasText(config.getMchCertSerialNo())
                    && StringUtils.hasText(config.getApiV3KeyEncrypted())
                    && StringUtils.hasText(config.getMchPrivateKeyEncrypted());
        }
        return StringUtils.hasText(config.getAppId())
                && StringUtils.hasText(config.getMchPrivateKeyEncrypted())
                && StringUtils.hasText(config.getAlipayPublicKeyEncrypted());
    }

    private String encryptOrKeep(String plaintext, String existingCiphertext, String fieldName) {
        if (StringUtils.hasText(plaintext)) {
            return aesGcmEncryptor.encrypt(plaintext.trim());
        }
        if (StringUtils.hasText(existingCiphertext)) {
            return existingCiphertext;
        }
        throw new BusinessException(ErrorCode.PARAM_INVALID, fieldName + "不能为空");
    }

    private String normalizeAlipayGateway(String gatewayUrl) {
        String value = StringUtils.hasText(gatewayUrl) ? gatewayUrl.trim() : DEFAULT_ALIPAY_GATEWAY;
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !ALIPAY_GATEWAY_HOSTS.contains(uri.getHost())) {
                throw new IllegalArgumentException();
            }
            return value;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "支付宝网关仅支持官方正式或沙箱地址");
        }
    }
}
