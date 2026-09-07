package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.mp.MpAuthorizerService;
import com.shopplatform.domain.mp.MpComponentService;
import com.shopplatform.domain.mp.entity.MpAuthorizer;
import com.shopplatform.framework.tenant.TenantContext;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/mp")
public class StoreMpController {

    private final MpAuthorizerService mpAuthorizerService;
    private final MpComponentService mpComponentService;

    public StoreMpController(MpAuthorizerService mpAuthorizerService, MpComponentService mpComponentService) {
        this.mpAuthorizerService = mpAuthorizerService;
        this.mpComponentService = mpComponentService;
    }

    @GetMapping
    public Result<Map<String, Object>> overview() {
        Long shopId = TenantContext.getRequired();
        var settings = mpComponentService.settings();
        List<Map<String, Object>> items = mpAuthorizerService.listByShop(shopId).stream()
                .map(this::toView)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("componentConfigured", settings.configured());
        data.put("ticketReady", !mpComponentService.latestTicket().isBlank());
        data.put("authorizers", items);
        return Result.ok(data);
    }

    @PostMapping("/self")
    public Result<Map<String, Object>> saveSelf(@RequestBody SelfBindRequest body) {
        MpAuthorizer row = mpAuthorizerService.saveSelf(
                TenantContext.getRequired(), body.appType(), body.appId(), body.appSecret());
        return Result.ok(toView(row));
    }

    @PostMapping("/auth-url")
    public Result<MpAuthorizerService.AuthUrl> authUrl(@RequestParam(defaultValue = "mini") String appType) {
        return Result.ok(mpAuthorizerService.startHostedAuth(TenantContext.getRequired(), appType));
    }

    @GetMapping("/ext-json")
    public Result<Map<String, Object>> extJson(@RequestParam(defaultValue = "mini") String appType) {
        return Result.ok(mpAuthorizerService.extJson(TenantContext.getRequired(), appType));
    }

    @DeleteMapping("/{appType}")
    public Result<Void> unbind(@PathVariable String appType) {
        mpAuthorizerService.unbind(TenantContext.getRequired(), appType);
        return Result.ok();
    }

    private Map<String, Object> toView(MpAuthorizer row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.getId());
        m.put("appType", row.getAppType());
        m.put("authMode", row.getAuthMode());
        m.put("appId", row.getAppid());
        m.put("authStatus", row.getAuthStatus());
        m.put("nickName", row.getNickName());
        m.put("onlineVersion", row.getOnlineVersion());
        m.put("auditStatus", row.getAuditStatus());
        m.put("authorizedTime", row.getAuthorizedTime());
        m.put("secretSet", row.getAppSecretEncrypted() != null && !row.getAppSecretEncrypted().isBlank());
        return m;
    }

    public record SelfBindRequest(
            @NotBlank String appType,
            @NotBlank String appId,
            String appSecret
    ) {
    }
}
