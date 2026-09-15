package com.shopplatform.domain.mp.wechat;

/** 微信小程序登录及手机号能力的服务端 API。 */
public interface WxMiniProgramClient {

    Session code2Session(String appId, String appSecret, String code);

    Session componentCode2Session(String appId, String code, String componentAppId,
                                  String componentAccessToken);

    Token accessToken(String appId, String appSecret);

    String phoneNumber(String accessToken, String code);

    record Session(String openId, String unionId) {
    }

    record Token(String accessToken, int expiresIn) {
    }
}
