package com.shopplatform.domain.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.pay.entity.ShopPayConfig;
import com.shopplatform.domain.pay.mapper.ShopPayConfigMapper;
import com.shopplatform.domain.pay.service.ShopPayConfigService;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class ShopPayConfigServiceImpl extends ServiceImpl<ShopPayConfigMapper, ShopPayConfig>
        implements ShopPayConfigService {

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
        config.setApiV3KeyEncrypted(aesGcmEncryptor.encrypt(cmd.apiV3Key()));
        config.setMchPrivateKeyEncrypted(aesGcmEncryptor.encrypt(cmd.mchPrivateKeyPem()));
        config.setStatus("enabled");
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
}
