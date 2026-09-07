package com.shopplatform.adminapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.mp.MpAuthorizerService;
import com.shopplatform.domain.mp.MpCodeTemplateService;
import com.shopplatform.domain.mp.MpComponentService;
import com.shopplatform.domain.mp.entity.MpAuthorizer;
import com.shopplatform.domain.mp.entity.MpCodeTemplate;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.service.ShopService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/mp")
public class AdminMpController {

    private final MpAuthorizerService mpAuthorizerService;
    private final MpCodeTemplateService mpCodeTemplateService;
    private final MpComponentService mpComponentService;
    private final ShopService shopService;

    public AdminMpController(MpAuthorizerService mpAuthorizerService,
                             MpCodeTemplateService mpCodeTemplateService,
                             MpComponentService mpComponentService,
                             ShopService shopService) {
        this.mpAuthorizerService = mpAuthorizerService;
        this.mpCodeTemplateService = mpCodeTemplateService;
        this.mpComponentService = mpComponentService;
        this.shopService = shopService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        var settings = mpComponentService.settings();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("componentConfigured", settings.configured());
        data.put("componentAppId", settings.componentAppId());
        data.put("ticketReady", !mpComponentService.latestTicket().isBlank());
        data.put("ticketReceivedTime", mpComponentService.ticketReceivedTime());
        data.put("callbackUrl", settings.callbackUrl());
        return Result.ok(data);
    }

    @GetMapping("/authorizers")
    public Result<List<Map<String, Object>>> authorizers() {
        Map<Long, Shop> shops = shopService.list().stream()
                .collect(Collectors.toMap(Shop::getId, s -> s, (a, b) -> a));
        List<Map<String, Object>> rows = mpAuthorizerService.listAll().stream()
                .map(row -> toAuthorizer(row, shops.get(row.getShopId())))
                .toList();
        return Result.ok(rows);
    }

    @GetMapping("/templates")
    public Result<List<MpCodeTemplate>> templates() {
        return Result.ok(mpCodeTemplateService.listAll());
    }

    @PostMapping("/templates")
    public Result<MpCodeTemplate> saveTemplate(@RequestBody TemplateRequest body) {
        return Result.ok(mpCodeTemplateService.save(body.templateId(), body.userVersion(), body.userDesc()));
    }

    @PutMapping("/templates/{id}/disable")
    public Result<Void> disableTemplate(@PathVariable Long id) {
        mpCodeTemplateService.disable(id);
        return Result.ok();
    }

    @GetMapping("/ext-json")
    public Result<Map<String, Object>> extJson(@RequestParam Long shopId,
                                               @RequestParam(defaultValue = "mini") String appType) {
        return Result.ok(mpAuthorizerService.extJson(shopId, appType));
    }

    private static Map<String, Object> toAuthorizer(MpAuthorizer row, Shop shop) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.getId());
        m.put("shopId", row.getShopId());
        m.put("shopName", shop == null ? "" : shop.getName());
        m.put("shopCode", shop == null ? "" : shop.getCode());
        m.put("appType", row.getAppType());
        m.put("authMode", row.getAuthMode());
        m.put("appId", row.getAppid());
        m.put("authStatus", row.getAuthStatus());
        m.put("nickName", row.getNickName());
        m.put("onlineVersion", row.getOnlineVersion());
        m.put("authorizedTime", row.getAuthorizedTime());
        return m;
    }

    public record TemplateRequest(
            @NotBlank String templateId,
            @NotBlank String userVersion,
            String userDesc
    ) {
    }
}
