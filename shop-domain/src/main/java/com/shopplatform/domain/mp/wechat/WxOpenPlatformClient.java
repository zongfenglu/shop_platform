package com.shopplatform.domain.mp.wechat;

import java.util.List;

/** 微信开放平台第三方 HTTP。未配置或微信返回 errcode 时由实现抛业务异常，不编造成功。 */
public interface WxOpenPlatformClient {

    Token componentToken(String componentAppId, String componentSecret, String ticket);

    String createPreAuthCode(String componentAccessToken, String componentAppId);

    Authorization queryAuth(String componentAccessToken, String componentAppId, String authCode);

    Token refreshAuthorizerToken(String componentAccessToken, String componentAppId,
                                 String authorizerAppId, String refreshToken);

    AuthorizerInfo getAuthorizerInfo(String componentAccessToken, String componentAppId, String authorizerAppId);

    record Token(String accessToken, int expiresIn) {
    }

    record Authorization(
            String authorizerAppId,
            String authorizerAccessToken,
            int expiresIn,
            String authorizerRefreshToken,
            String funcInfoJson
    ) {
    }

    record AuthorizerInfo(String nickName, String appId, String userName, List<Integer> funcIds) {
    }
}
