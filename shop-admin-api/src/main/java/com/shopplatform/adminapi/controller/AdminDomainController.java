package com.shopplatform.adminapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.adminapi.dto.DomainItem;
import com.shopplatform.adminapi.dto.DomainListQuery;
import com.shopplatform.adminapi.dto.DomainPageResponse;
import com.shopplatform.adminapi.dto.DomainSummary;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.service.ShopDomainBindingService;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.ssl.SslCertificateService;
import com.shopplatform.framework.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台域名管理。对应原型 admin/domain-list.html。
 * 审核通过后走 Let's Encrypt HTTP-01；未启用 ACME 时证书保持 pending，不假装已签发。
 */
@RestController
@RequestMapping("/admin/domains")
public class AdminDomainController {

    private final ShopDomainService shopDomainService;
    private final ShopDomainBindingService shopDomainBindingService;
    private final ShopService shopService;
    private final SysLogService sysLogService;
    private final SslCertificateService sslCertificateService;

    public AdminDomainController(ShopDomainService shopDomainService,
                                 ShopDomainBindingService shopDomainBindingService,
                                 ShopService shopService,
                                 SysLogService sysLogService,
                                 SslCertificateService sslCertificateService) {
        this.shopDomainService = shopDomainService;
        this.shopDomainBindingService = shopDomainBindingService;
        this.shopService = shopService;
        this.sysLogService = sysLogService;
        this.sslCertificateService = sslCertificateService;
    }

    @GetMapping
    public Result<DomainPageResponse> page(DomainListQuery query) {
        var wrapper = Wrappers.<ShopDomain>lambdaQuery();
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();
            List<Long> shopIds = shopService.list(
                    Wrappers.<Shop>lambdaQuery()
                            .and(w -> w.like(Shop::getName, keyword)
                                    .or().like(Shop::getCode, keyword)))
                    .stream()
                    .map(Shop::getId)
                    .toList();
            wrapper.and(w -> {
                w.like(ShopDomain::getDomain, keyword);
                if (!shopIds.isEmpty()) {
                    w.or().in(ShopDomain::getShopId, shopIds);
                }
            });
        }
        if (StringUtils.hasText(query.type())) {
            wrapper.eq(ShopDomain::getType, query.type());
        }
        if (StringUtils.hasText(query.verifyStatus())) {
            wrapper.eq(ShopDomain::getVerifyStatus, query.verifyStatus());
        }
        wrapper.orderByDesc(ShopDomain::getCreateTime);

        Page<ShopDomain> page = shopDomainService.page(
                new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault()), wrapper);

        List<DomainItem> records = toItems(page.getRecords());
        DomainSummary summary = buildSummary();
        return Result.ok(new DomainPageResponse(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), summary));
    }

    @PostMapping("/{id}/cname-check")
    public Result<DomainItem> checkCname(@PathVariable Long id) {
        ShopDomain row = shopDomainBindingService.checkCname(null, id);
        return Result.ok(toItem(row));
    }

    @PostMapping("/{id}/approve")
    public Result<DomainItem> approve(@PathVariable Long id, HttpServletRequest request) {
        ShopDomain row = shopDomainBindingService.approve(id);
        sysLogService.record(row.getShopId(), "domain-approve", "审核通过 " + row.getDomain(), ClientIp.resolve(request));
        return Result.ok(toItem(row));
    }

    @PostMapping("/{id}/reject")
    public Result<DomainItem> reject(@PathVariable Long id,
                                     @RequestBody(required = false) RejectRequest body,
                                     HttpServletRequest request) {
        String reason = body == null ? null : body.reason();
        ShopDomain row = shopDomainBindingService.reject(id, reason);
        sysLogService.record(row.getShopId(), "domain-reject", "驳回 " + row.getDomain(), ClientIp.resolve(request));
        return Result.ok(toItem(row));
    }

    @PostMapping("/{id}/unbind")
    public Result<Void> unbind(@PathVariable Long id) {
        shopDomainBindingService.unbind(null, id);
        return Result.ok();
    }

    @PostMapping("/{id}/issue-cert")
    public Result<DomainItem> issueCert(@PathVariable Long id, HttpServletRequest request) {
        ShopDomain row = sslCertificateService.issueNow(id);
        sysLogService.record(row.getShopId(), "domain-issue-cert", "签发证书 " + row.getDomain(), ClientIp.resolve(request));
        return Result.ok(toItem(row));
    }

    @PostMapping("/{id}/certificate")
    public Result<DomainItem> uploadCertificate(@PathVariable Long id,
                                                @RequestParam("certificate") MultipartFile certificate,
                                                @RequestParam("privateKey") MultipartFile privateKey,
                                                HttpServletRequest request) {
        ShopDomain row = sslCertificateService.upload(id, certificate, privateKey);
        sysLogService.record(row.getShopId(), "domain-upload-cert", "上传证书 " + row.getDomain(), ClientIp.resolve(request));
        return Result.ok(toItem(row));
    }

    @PutMapping("/{id}/protocol")
    public Result<DomainItem> updateProtocol(@PathVariable Long id, @RequestBody ProtocolRequest body) {
        ShopDomain row = shopDomainBindingService.updateProtocol(null, id, body == null ? null : body.protocol());
        return Result.ok(toItem(row));
    }

    private DomainSummary buildSummary() {
        long total = shopDomainService.count();
        long customCount = shopDomainService.count(Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getType, "custom"));
        long subCount = shopDomainService.count(Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getType, "sub"));
        long pendingCount = shopDomainService.count(Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getVerifyStatus, "pending"));
        long validCount = shopDomainService.count(Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getCertStatus, "valid"));
        return new DomainSummary(total, customCount, subCount, pendingCount, validCount);
    }

    private List<DomainItem> toItems(List<ShopDomain> domains) {
        if (domains == null || domains.isEmpty()) {
            return List.of();
        }
        Map<Long, Shop> shopMap = shopService.listByIds(domains.stream().map(ShopDomain::getShopId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Shop::getId, item -> item));
        return domains.stream().map(domain -> toItem(domain, shopMap.get(domain.getShopId()))).toList();
    }

    private DomainItem toItem(ShopDomain domain) {
        Shop shop = shopService.getOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getId, domain.getShopId()));
        return toItem(domain, shop);
    }

    private DomainItem toItem(ShopDomain domain, Shop shop) {
        return new DomainItem(
                domain.getId(),
                domain.getShopId(),
                shop == null ? "未知商家" : shop.getName(),
                domain.getDomain(),
                domain.getProtocol(),
                domain.getType(),
                domain.getCertStatus(),
                domain.getCertExpireTime(),
                domain.getVerifyStatus(),
                domain.getCnameTarget(),
                domain.getCnameStatus(),
                domain.getRejectReason(),
                domain.getCertError(),
                domain.getCreateTime()
        );
    }

    public record RejectRequest(@Size(max = 255) String reason) {
    }

    public record ProtocolRequest(String protocol) { }
}
