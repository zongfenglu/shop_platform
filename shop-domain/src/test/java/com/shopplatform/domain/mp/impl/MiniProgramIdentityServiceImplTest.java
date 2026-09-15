package com.shopplatform.domain.mp.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.mp.MpAuthorizerService;
import com.shopplatform.domain.mp.MpComponentService;
import com.shopplatform.domain.mp.entity.MpAuthorizer;
import com.shopplatform.domain.mp.wechat.WxMiniProgramClient;
import com.shopplatform.domain.mp.wechat.WxOpenPlatformClient;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniProgramIdentityServiceImplTest {

    private MpAuthorizerService authorizerService;
    private WxMiniProgramClient miniProgramClient;
    private StringRedisTemplate redisTemplate;
    private MiniProgramIdentityServiceImpl service;
    private AesGcmEncryptor encryptor;

    @BeforeEach
    void setUp() {
        authorizerService = mock(MpAuthorizerService.class);
        miniProgramClient = mock(WxMiniProgramClient.class);
        redisTemplate = mock(StringRedisTemplate.class);
        encryptor = new AesGcmEncryptor("unit-test-mini-program-key");
        service = new MiniProgramIdentityServiceImpl(
                authorizerService,
                mock(MpComponentService.class),
                miniProgramClient,
                mock(WxOpenPlatformClient.class),
                encryptor,
                redisTemplate);
    }

    @Test
    void exchangeLoginCode_usesCurrentShopsSelfManagedAppSecret() {
        MpAuthorizer auth = selfAuthorizer();
        when(authorizerService.findByShopAndType(1001L, "mini")).thenReturn(auth);
        when(miniProgramClient.code2Session("wx-test", "app-secret", "login-code"))
                .thenReturn(new WxMiniProgramClient.Session("openid-1", "unionid-1"));

        var identity = service.exchangeLoginCode(1001L, "wx-test", "login-code");

        assertEquals("openid-1", identity.openId());
        assertEquals("unionid-1", identity.unionId());
    }

    @Test
    void exchangeLoginCode_rejectsAppIdNotBoundToCurrentShop() {
        when(authorizerService.findByShopAndType(1001L, "mini")).thenReturn(selfAuthorizer());

        assertThrows(BusinessException.class,
                () -> service.exchangeLoginCode(1001L, "wx-other", "login-code"));
        verify(miniProgramClient, never()).code2Session(anyString(), anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exchangePhoneCode_usesCachedAccessToken() {
        MpAuthorizer auth = selfAuthorizer();
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(authorizerService.findByShopAndType(1001L, "mini")).thenReturn(auth);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("wx:mini:access_token:wx-test")).thenReturn("access-token");
        when(miniProgramClient.phoneNumber("access-token", "phone-code")).thenReturn("13800138000");

        assertEquals("13800138000", service.exchangePhoneCode(1001L, "wx-test", "phone-code"));
    }

    private MpAuthorizer selfAuthorizer() {
        MpAuthorizer auth = new MpAuthorizer();
        auth.setShopId(1001L);
        auth.setAppType("mini");
        auth.setAuthMode("self");
        auth.setAppid("wx-test");
        auth.setAppSecretEncrypted(encryptor.encrypt("app-secret"));
        auth.setAuthStatus("authorized");
        return auth;
    }
}
