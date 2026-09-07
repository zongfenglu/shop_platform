package com.shopplatform.domain.mp;

import com.shopplatform.domain.mp.wechat.WxMsgCrypt;
import com.shopplatform.domain.mp.wechat.WxOpenPlatformClient;

import java.time.LocalDateTime;

public interface MpComponentService {

    MpWechatSettings settings();

    void requireConfigured();

    String latestTicket();

    LocalDateTime ticketReceivedTime();

    void saveTicket(String ticket);

    /** 处理微信推送的明文 XML（已验签解密）。 */
    String handleEventXml(String xml);

    WxMsgCrypt crypt();

    String componentAccessToken();

    /** 刷新 component_access_token；无票据或微信失败时抛业务异常。 */
    WxOpenPlatformClient.Token refreshComponentToken();

    String createPreAuthCode();
}
