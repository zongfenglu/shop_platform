package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.WechatCodeRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.mp.MiniProgramIdentityService;
import com.shopplatform.framework.security.JwtTokenProvider;
import com.shopplatform.framework.security.LoginUserContext;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerAuthControllerTest {

    private MemberService memberService;
    private MiniProgramIdentityService identityService;
    private ConsumerAuthController controller;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        identityService = mock(MiniProgramIdentityService.class);
        controller = new ConsumerAuthController(memberService, identityService,
                new JwtTokenProvider("test_secret_key_that_is_long_enough_for_hmac_sha256_algorithm", 12));
        TenantContext.set(1001L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        LoginUserContext.clear();
    }

    @Test
    void wechatLogin_createsTokenWithoutRequiringMobile() {
        when(identityService.exchangeLoginCode(1001L, "wx-test", "login-code"))
                .thenReturn(new MiniProgramIdentityService.WechatIdentity("openid-1", "unionid-1"));
        Member member = member(20L, null);
        when(memberService.loginOrRegisterWechat("openid-1", "unionid-1")).thenReturn(member);

        var response = controller.wechatLogin("wx-test", new WechatCodeRequest("login-code")).getData();

        assertEquals(20L, response.userId());
        assertNull(response.mobile());
        assertTrue(response.token().split("\\.").length == 3);
        verify(memberService).loginOrRegisterWechat("openid-1", "unionid-1");
    }

    @Test
    void bindWechatPhone_requiresWechatLogin() {
        assertThrows(BusinessException.class,
                () -> controller.bindWechatPhone("wx-test", new WechatCodeRequest("phone-code")));
    }

    @Test
    void bindWechatPhone_returnsUpdatedMember() {
        LoginUserContext.set(new LoginUserContext.LoginUser(20L, 1001L, null, false));
        when(identityService.exchangePhoneCode(1001L, "wx-test", "phone-code"))
                .thenReturn("13800138000");
        when(memberService.bindMobile(20L, "13800138000")).thenReturn(member(20L, "13800138000"));

        var response = controller.bindWechatPhone("wx-test", new WechatCodeRequest("phone-code")).getData();

        assertEquals("13800138000", response.mobile());
    }

    private Member member(Long id, String mobile) {
        Member member = new Member();
        member.setId(id);
        member.setNickname("微信用户");
        member.setMobile(mobile);
        return member;
    }
}
