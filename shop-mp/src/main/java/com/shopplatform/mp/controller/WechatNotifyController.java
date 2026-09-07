package com.shopplatform.mp.controller;

import com.shopplatform.domain.mp.MpAuthorizerService;
import com.shopplatform.domain.mp.MpComponentService;
import com.shopplatform.domain.mp.wechat.WxMsgCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信第三方平台授权事件与扫码回调。无 JWT，靠消息签名。
 */
@RestController
@RequestMapping("/notify/wechat")
public class WechatNotifyController {

    private static final Logger log = LoggerFactory.getLogger(WechatNotifyController.class);
    private static final Pattern ENCRYPT = Pattern.compile("<Encrypt><!\\[CDATA\\[(.*?)]]></Encrypt>|<Encrypt>([^<]+)</Encrypt>");

    private final MpComponentService mpComponentService;
    private final MpAuthorizerService mpAuthorizerService;
    private final String storeConsoleUrl;

    public WechatNotifyController(MpComponentService mpComponentService,
                                  MpAuthorizerService mpAuthorizerService,
                                  @Value("${shop.store-console-url:http://localhost:5174}") String storeConsoleUrl) {
        this.mpComponentService = mpComponentService;
        this.mpAuthorizerService = mpAuthorizerService;
        this.storeConsoleUrl = storeConsoleUrl;
    }

    @GetMapping(value = "/component", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(
            @RequestParam String signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam(required = false) String echostr) {
        return mpComponentService.crypt().verifyUrl(signature, timestamp, nonce, echostr);
    }

    @PostMapping(value = "/component", produces = MediaType.TEXT_PLAIN_VALUE)
    public String event(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestBody String body) {
        String encrypt = extractEncrypt(body);
        WxMsgCrypt crypt = mpComponentService.crypt();
        String xml = crypt.decrypt(msgSignature, timestamp, nonce, encrypt);
        log.info("微信第三方事件已解密");
        return mpComponentService.handleEventXml(xml);
    }

    @GetMapping("/auth/callback")
    public RedirectView authCallback(
            @RequestParam("auth_code") String authCode,
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "mini") String appType) {
        mpAuthorizerService.completeHostedAuth(shopId, appType, authCode);
        return new RedirectView(storeConsoleUrl + "/client?mp=ok");
    }

    static String extractEncrypt(String body) {
        if (body == null) {
            return "";
        }
        Matcher m = ENCRYPT.matcher(body);
        if (!m.find()) {
            return body.trim();
        }
        return m.group(1) != null ? m.group(1) : m.group(2);
    }
}
