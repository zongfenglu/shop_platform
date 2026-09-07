package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.ConsumerLoginRequest;
import com.shopplatform.clientapi.dto.ConsumerLoginResponse;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.service.MemberService;
import com.shopplatform.framework.security.JwtTokenProvider;
import com.shopplatform.framework.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ConsumerAuthController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    public ConsumerAuthController(MemberService memberService, JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public Result<ConsumerLoginResponse> login(@Valid @RequestBody ConsumerLoginRequest request) {
        Member member = memberService.loginOrRegister(request.mobile());
        String token = jwtTokenProvider.generate(String.valueOf(member.getId()),
                Map.of("shopId", TenantContext.getRequired()));
        return Result.ok(new ConsumerLoginResponse(token, member.getId(), member.getNickname()));
    }
}
