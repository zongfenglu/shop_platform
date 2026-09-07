package com.shopplatform.adminapi.controller;

import com.shopplatform.adminapi.dto.LoginRequest;
import com.shopplatform.adminapi.dto.LoginResponse;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.platform.entity.PlatformUser;
import com.shopplatform.domain.platform.service.PlatformUserService;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.framework.security.JwtTokenProvider;
import com.shopplatform.framework.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 平台超管登录。对应原型 admin/login.html。
 * <p>
 * 平台管理员登录不涉及租户上下文（默认无租户，见文档三 §2.2），
 * 生成的 JWT 只含 sub=userId，不带 shopId。
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final PlatformUserService platformUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysLogService sysLogService;

    public AdminAuthController(PlatformUserService platformUserService,
                                PasswordEncoder passwordEncoder,
                                JwtTokenProvider jwtTokenProvider,
                                SysLogService sysLogService) {
        this.platformUserService = platformUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysLogService = sysLogService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        PlatformUser user = platformUserService.findByUsername(request.username());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在或已停用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        platformUserService.touchLastLogin(user.getId());
        sysLogService.record(null, 1, user.getId(), user.getUsername(), false, "login", "账号密码登录", ClientIp.resolve(httpRequest));

        String token = jwtTokenProvider.generate(
                String.valueOf(user.getId()),
                Map.of("username", user.getUsername()));

        return Result.ok(new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName()));
    }
}
