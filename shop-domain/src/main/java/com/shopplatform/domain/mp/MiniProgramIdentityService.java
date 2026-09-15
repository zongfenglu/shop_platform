package com.shopplatform.domain.mp;

/** 按当前店铺的小程序配置交换微信身份与手机号。 */
public interface MiniProgramIdentityService {

    WechatIdentity exchangeLoginCode(Long shopId, String appId, String code);

    String exchangePhoneCode(Long shopId, String appId, String code);

    record WechatIdentity(String openId, String unionId) {
    }
}
