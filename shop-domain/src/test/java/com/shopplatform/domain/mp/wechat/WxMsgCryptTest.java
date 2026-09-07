package com.shopplatform.domain.mp.wechat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WxMsgCryptTest {

    private static final String TOKEN = "shopToken";
    private static final String AES_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
    private static final String APP_ID = "wxcomponentappid00";

    @Test
    void encryptDecryptRoundTrip() {
        WxMsgCrypt crypt = new WxMsgCrypt(TOKEN, AES_KEY, APP_ID);
        String xml = "<xml><InfoType><![CDATA[component_verify_ticket]]></InfoType>"
                + "<ComponentVerifyTicket><![CDATA[ticket@@@demo]]></ComponentVerifyTicket></xml>";
        String encrypt = crypt.encrypt(xml);
        String ts = "1710000000";
        String nonce = "nonce1";
        String sign = crypt.msgSignature(ts, nonce, encrypt);
        String plain = crypt.decrypt(sign, ts, nonce, encrypt);
        assertTrue(plain.contains("ticket@@@demo"));
        assertTrue(plain.contains("component_verify_ticket"));
    }

    @Test
    void verifyUrlEchoesWhenPlainSignatureMatches() {
        WxMsgCrypt crypt = new WxMsgCrypt(TOKEN, AES_KEY, APP_ID);
        String ts = "1710000000";
        String nonce = "abc";
        String echo = "ping";
        assertEquals(echo, crypt.verifyUrl(crypt.plainSignature(ts, nonce), ts, nonce, echo));
    }
}
