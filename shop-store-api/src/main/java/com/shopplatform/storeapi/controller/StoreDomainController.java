package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.service.ShopDomainBindingService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商户端域名。对应原型 store/client.html 的 H5 Tab：
 * 平台泛域名建店即生效；自定义域名提交后由超管审核。
 */
@RestController
@RequestMapping("/store/domains")
public class StoreDomainController {

    private final ShopDomainBindingService shopDomainBindingService;
    private final ShopService shopService;
    private final String h5PublicUrl;

    public StoreDomainController(ShopDomainBindingService shopDomainBindingService,
                                 ShopService shopService,
                                 @Value("${shop.h5-public-url:http://localhost:5175}") String h5PublicUrl) {
        this.shopDomainBindingService = shopDomainBindingService;
        this.shopService = shopService;
        this.h5PublicUrl = h5PublicUrl.endsWith("/")
                ? h5PublicUrl.substring(0, h5PublicUrl.length() - 1)
                : h5PublicUrl;
    }

    @GetMapping
    public Result<Overview> list() {
        Long shopId = TenantContext.getRequired();
        Shop shop = shopService.getOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getId, shopId));
        String defaultDomain = shop == null ? null : shopDomainBindingService.defaultHost(shop);
        return Result.ok(new Overview(
                shopDomainBindingService.platformBaseDomain(),
                h5PublicUrl,
                shopDomainBindingService.skipCname(),
                defaultDomain,
                shopDomainBindingService.listByShopId(shopId)
        ));
    }

    @PostMapping
    public Result<ShopDomain> apply(@Valid @RequestBody ApplyRequest request) {
        return Result.ok(shopDomainBindingService.applyCustom(TenantContext.getRequired(), request.domain()));
    }

    @PostMapping("/{id}/cname-check")
    public Result<ShopDomain> checkCname(@PathVariable Long id) {
        return Result.ok(shopDomainBindingService.checkCname(TenantContext.getRequired(), id));
    }

    @PutMapping("/{id}/protocol")
    public Result<ShopDomain> updateProtocol(@PathVariable Long id, @RequestBody ProtocolRequest request) {
        return Result.ok(shopDomainBindingService.updateProtocol(TenantContext.getRequired(), id,
                request == null ? null : request.protocol()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> unbind(@PathVariable Long id) {
        shopDomainBindingService.unbind(TenantContext.getRequired(), id);
        return Result.ok();
    }

    public record ApplyRequest(@NotBlank @Size(max = 128) String domain) {
    }

    public record ProtocolRequest(String protocol) { }

    public record Overview(
            String platformBaseDomain,
            String h5PublicUrl,
            boolean skipCname,
            String defaultDomain,
            List<ShopDomain> domains
    ) {
    }
}
