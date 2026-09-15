package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.ConsumerLoginRequest;
import com.shopplatform.clientapi.dto.ConsumerLoginResponse;
import com.shopplatform.clientapi.dto.WechatCodeRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.domain.mp.MiniProgramIdentityService;
import com.shopplatform.framework.security.LoginUserContext;
import com.shopplatform.framework.security.JwtTokenProvider;
import com.shopplatform.framework.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ConsumerAuthController {

    private final MemberService memberService;
    private final MiniProgramIdentityService miniProgramIdentityService;
    private final JwtTokenProvider jwtTokenProvider;

    public ConsumerAuthController(MemberService memberService,
                                  MiniProgramIdentityService miniProgramIdentityService,
                                  JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.miniProgramIdentityService = miniProgramIdentityService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public Result<ConsumerLoginResponse> login(@Valid @RequestBody ConsumerLoginRequest request) {
        Member member = memberService.loginOrRegister(request.mobile());
        return Result.ok(loginResponse(member));
    }

    /** wx.login 的临时 code 换取 openId，首次访问自动创建会员，不要求手机号。 */
    @PostMapping("/wechat/login")
    public Result<ConsumerLoginResponse> wechatLogin(
            @RequestHeader("X-Mini-AppId") String appId,
            @Valid @RequestBody WechatCodeRequest request) {
        var identity = miniProgramIdentityService.exchangeLoginCode(
                TenantContext.getRequired(), appId, request.code());
        Member member = memberService.loginOrRegisterWechat(identity.openId(), identity.unionId());
        return Result.ok(loginResponse(member));
    }

    /** 微信原生 getPhoneNumber 动态 code 换手机号，并绑定到已登录会员。 */
    @PostMapping("/wechat/phone")
    public Result<ConsumerLoginResponse> bindWechatPhone(
            @RequestHeader("X-Mini-AppId") String appId,
            @Valid @RequestBody WechatCodeRequest request) {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先完成微信登录");
        }
        String mobile = miniProgramIdentityService.exchangePhoneCode(
                TenantContext.getRequired(), appId, request.code());
        Member member = memberService.bindMobile(loginUser.userId(), mobile);
        return Result.ok(loginResponse(member));
    }

    private ConsumerLoginResponse loginResponse(Member member) {
        String token = jwtTokenProvider.generate(String.valueOf(member.getId()),
                Map.of("shopId", TenantContext.getRequired()));
        return new ConsumerLoginResponse(token, member.getId(), member.getNickname(), member.getMobile());
    }
}
