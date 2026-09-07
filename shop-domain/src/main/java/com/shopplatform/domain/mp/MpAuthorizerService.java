package com.shopplatform.domain.mp;

import com.shopplatform.domain.mp.entity.MpAuthorizer;

import java.util.List;
import java.util.Map;

public interface MpAuthorizerService {

    List<MpAuthorizer> listByShop(Long shopId);

    List<MpAuthorizer> listAll();

    MpAuthorizer findByShopAndType(Long shopId, String appType);

    Long findShopIdByAppId(String appId);

    MpAuthorizer saveSelf(Long shopId, String appType, String appId, String appSecret);

    void unbind(Long shopId, String appType);

    AuthUrl startHostedAuth(Long shopId, String appType);

    MpAuthorizer completeHostedAuth(Long shopId, String appType, String authCode);

    Map<String, Object> extJson(Long shopId, String appType);

    record AuthUrl(String url, String preAuthCode, String redirectUri) {
    }
}
