package com.shopplatform.domain.mp.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.mp.MiniProgramIdentityService;
import com.shopplatform.domain.mp.MpAuthorizerService;
import com.shopplatform.domain.mp.MpComponentService;
import com.shopplatform.domain.mp.MpWechatSettings;
import com.shopplatform.domain.mp.entity.MpAuthorizer;
import com.shopplatform.domain.mp.wechat.WxMiniProgramClient;
import com.shopplatform.domain.mp.wechat.WxOpenPlatformClient;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
public class MiniProgramIdentityServiceImpl implements MiniProgramIdentityService {

    private static final String ACCESS_TOKEN_PREFIX = "wx:mini:access_token:";

    private final MpAuthorizerService authorizerService;
    private final MpComponentService componentService;
    private final WxMiniProgramClient miniProgramClient;
    private final WxOpenPlatformClient openPlatformClient;
    private final AesGcmEncryptor encryptor;
    private final StringRedisTemplate redisTemplate;

    public MiniProgramIdentityServiceImpl(MpAuthorizerService authorizerService,
                                          MpComponentService componentService,
                                          WxMiniProgramClient miniProgramClient,
                                          WxOpenPlatformClient openPlatformClient,
                                          AesGcmEncryptor encryptor,
                                          StringRedisTemplate redisTemplate) {
        this.authorizerService = authorizerService;
        this.componentService = componentService;
        this.miniProgramClient = miniProgramClient;
        this.openPlatformClient = openPlatformClient;
        this.encryptor = encryptor;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public WechatIdentity exchangeLoginCode(Long shopId, String appId, String code) {
        requireCode(code);
        MpAuthorizer auth = requireAuthorizer(shopId, appId);
        WxMiniProgramClient.Session session;
        if ("hosted".equals(auth.getAuthMode())) {
            MpWechatSettings settings = componentService.settings();
            componentService.requireConfigured();
            session = miniProgramClient.componentCode2Session(auth.getAppid(), code.trim(),
                    settings.componentAppId(), componentService.componentAccessToken());
        } else {
            session = miniProgramClient.code2Session(auth.getAppid(), selfSecret(auth), code.trim());
        }
        return new WechatIdentity(session.openId(), session.unionId());
    }

    @Override
    public String exchangePhoneCode(Long shopId, String appId, String code) {
        requireCode(code);
        MpAuthorizer auth = requireAuthorizer(shopId, appId);
        return miniProgramClient.phoneNumber(accessToken(auth), code.trim());
    }

    private String accessToken(MpAuthorizer auth) {
        String cacheKey = ACCESS_TOKEN_PREFIX + auth.getAppid();
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        String token;
        int expiresIn;
        if ("hosted".equals(auth.getAuthMode())) {
            if (!StringUtils.hasText(auth.getRefreshTokenEncrypted())) {
                throw new BusinessException(ErrorCode.MP_AUTHORIZER_NOT_FOUND, "小程序托管授权已失效");
            }
            MpWechatSettings settings = componentService.settings();
            componentService.requireConfigured();
            WxOpenPlatformClient.Token refreshed = openPlatformClient.refreshAuthorizerToken(
                    componentService.componentAccessToken(), settings.componentAppId(), auth.getAppid(),
                    encryptor.decrypt(auth.getRefreshTokenEncrypted()));
            token = refreshed.accessToken();
            expiresIn = refreshed.expiresIn();
        } else {
            WxMiniProgramClient.Token refreshed = miniProgramClient.accessToken(auth.getAppid(), selfSecret(auth));
            token = refreshed.accessToken();
            expiresIn = refreshed.expiresIn();
        }
        redisTemplate.opsForValue().set(cacheKey, token, Duration.ofSeconds(Math.max(60, expiresIn - 200)));
        return token;
    }

    private MpAuthorizer requireAuthorizer(Long shopId, String appId) {
        if (!StringUtils.hasText(appId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "缺少小程序 AppID");
        }
        MpAuthorizer auth = authorizerService.findByShopAndType(shopId, "mini");
        if (auth == null || !"authorized".equals(auth.getAuthStatus()) || !appId.trim().equals(auth.getAppid())) {
            throw new BusinessException(ErrorCode.MP_AUTHORIZER_NOT_FOUND, "当前商城未绑定该小程序");
        }
        return auth;
    }

    private String selfSecret(MpAuthorizer auth) {
        if (!StringUtils.hasText(auth.getAppSecretEncrypted())) {
            throw new BusinessException(ErrorCode.MP_AUTHORIZER_NOT_FOUND, "小程序 AppSecret 未配置");
        }
        return encryptor.decrypt(auth.getAppSecretEncrypted());
    }

    private void requireCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "微信授权 code 不能为空");
        }
    }
}
