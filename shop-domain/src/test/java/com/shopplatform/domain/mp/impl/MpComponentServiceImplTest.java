package com.shopplatform.domain.mp.impl;

import com.shopplatform.domain.mp.MpWechatSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpComponentServiceImplTest {

    @Test
    void parseXmlReadsCdataAndPlain() {
        Map<String, String> tags = MpComponentServiceImpl.parseXml(
                "<xml><InfoType><![CDATA[component_verify_ticket]]></InfoType>"
                        + "<ComponentVerifyTicket><![CDATA[ticket@@@x]]></ComponentVerifyTicket>"
                        + "<CreateTime>1</CreateTime></xml>");
        assertEquals("component_verify_ticket", tags.get("InfoType"));
        assertEquals("ticket@@@x", tags.get("ComponentVerifyTicket"));
        assertEquals("1", tags.get("CreateTime"));
    }

    @Test
    void settingsConfiguredRequiresAesKeyLength() {
        assertFalse(new MpWechatSettings("wx1", "s", "t", "short", "http://x").configured());
        assertTrue(new MpWechatSettings(
                "wx1", "s", "t", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG",
                "http://localhost:8085/notify/wechat/component").configured());
    }

    @Test
    void authCallbackUrlDerivedFromComponentUrl() {
        String url = new MpWechatSettings("a", "b", "c", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG",
                "http://localhost:8085/notify/wechat/component").authCallbackUrl();
        assertEquals("http://localhost:8085/notify/wechat/auth/callback", url);
    }
}
