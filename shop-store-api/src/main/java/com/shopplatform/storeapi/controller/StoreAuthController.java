package com.shopplatform.storeapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.service.ImpersonateTicketService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.shop.service.StoreUserService;
import com.shopplatform.framework.tenant.TenantContext;
import com.shopplatform.framework.security.JwtTokenProvider;
import com.shopplatform.framework.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import com.shopplatform.storeapi.dto.StoreLoginRequest;
import com.shopplatform.storeapi.dto.StoreLoginResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商户后台登录。对应原型 store/login.html。
 * <p>
 * 登录本身是"先不知道 shopId 就要查 shop_id 相关表"的特殊场景——见文档三 §2.2：
 * 真实生产环境应由 Host 头（huajianji.shop.com）反查 shopId，
 * 本接口用请求体里的 shopCode 模拟同样的效果，方便在没有真实多域名网关的本地/测试环境下跑通登录。
 */
@RestController
@RequestMapping("/store/auth")
public class StoreAuthController {

    private final ShopService shopService;
    private final StoreUserService storeUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final ImpersonateTicketService impersonateTicketService;
    private final SysLogService sysLogService;

    public StoreAuthController(ShopService shopService,
                                StoreUserService storeUserService,
                                PasswordEncoder passwordEncoder,
                                JwtTokenProvider jwtTokenProvider,
                                ImpersonateTicketService impersonateTicketService,
                                SysLogService sysLogService) {
        this.shopService = shopService;
        this.storeUserService = storeUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.impersonateTicketService = impersonateTicketService;
        this.sysLogService = sysLogService;
    }

    @PostMapping("/login")
    public Result<StoreLoginResponse> login(@Valid @RequestBody StoreLoginRequest request, HttpServletRequest httpRequest) {
        Shop shop = shopService.findByCode(request.shopCode());
        if (shop == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "商城不存在");
        }
        if (!shop.getStatus().equals("trial") && !shop.getStatus().equals("normal")
                && !shop.getStatus().equals("expired")) {
            // 已停用/已归档禁止登录；已过期仍允许登录看数据/续费/发货，见文档一 §3.3
            throw new BusinessException(ErrorCode.TENANT_DISABLED, "商城已停用，无法登录");
        }

        StoreUser user = storeUserService.findByUsername(shop.getId(), request.username());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在或已停用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        storeUserService.touchLastLogin(user.getId());
        sysLogService.record(shop.getId(), 2, user.getId(), user.getUsername(), false, "login", "账号密码登录", ClientIp.resolve(httpRequest));

        String token = jwtTokenProvider.generate(
                String.valueOf(user.getId()),
                Map.of("shopId", shop.getId(), "username", user.getUsername(), "platformImpersonation", false));

        return Result.ok(new StoreLoginResponse(token, shop.getId(), shop.getCode(), user.getId(), user.getUsername(), user.getRealName(), false));
    }

    @PostMapping("/impersonate")
    public Result<StoreLoginResponse> impersonate(@Valid @RequestBody ImpersonateRequest request, HttpServletRequest httpRequest) {
        ImpersonateTicketService.Payload payload = impersonateTicketService.consume(request.ticket());
        if (payload == null) {
            throw new BusinessException(ErrorCode.IMPERSONATE_TICKET_INVALID, "免密登录已失效，请重新发起");
        }
        Shop shop = shopService.getByIdWithTenant(payload.shopId());
        if (shop == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "商城不存在");
        }
        String status = shop.getStatus();
        if (!"trial".equals(status) && !"normal".equals(status) && !"expired".equals(status)) {
            throw new BusinessException(ErrorCode.TENANT_DISABLED, "商城已停用，无法登录");
        }
        TenantContext.set(payload.shopId());
        StoreUser user;
        try {
            user = storeUserService.getByIdWithTenant(payload.storeUserId());
        } finally {
            TenantContext.clear();
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在或已停用");
        }
        storeUserService.touchLastLogin(user.getId());
        sysLogService.record(shop.getId(), 2, user.getId(), user.getUsername(), true, "login", "平台代管理登录", ClientIp.resolve(httpRequest));
        String token = jwtTokenProvider.generate(
                String.valueOf(user.getId()),
                Map.of("shopId", shop.getId(), "username", user.getUsername(), "platformImpersonation", true));
        return Result.ok(new StoreLoginResponse(
                token, shop.getId(), shop.getCode(), user.getId(), user.getUsername(), user.getRealName(), true));
    }

    public record ImpersonateRequest(@NotBlank String ticket) {
    }
}
