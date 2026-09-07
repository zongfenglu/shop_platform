package com.shopplatform.domain.shop.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.dns.CnameLookup;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.ssl.SslCertificateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 自定义域名申请 / CNAME 校验 / 平台审核。
 * 审核通过后触发 Let's Encrypt HTTP-01；未启用 ACME 时保持 cert_status=pending，不假装已签发。
 */
@Service
public class ShopDomainBindingService {

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}$");

    private final ShopDomainService shopDomainService;
    private final ShopService shopService;
    private final CnameLookup cnameLookup;
    private final SslCertificateService sslCertificateService;
    private final String platformBaseDomain;
    private final boolean skipCname;

    public ShopDomainBindingService(ShopDomainService shopDomainService,
                                    ShopService shopService,
                                    CnameLookup cnameLookup,
                                    SslCertificateService sslCertificateService,
                                    @Value("${shop.platform.base-domain:shop.com}") String platformBaseDomain,
                                    @Value("${shop.domain.skip-cname:true}") boolean skipCname) {
        this.shopDomainService = shopDomainService;
        this.shopService = shopService;
        this.cnameLookup = cnameLookup;
        this.sslCertificateService = sslCertificateService;
        this.platformBaseDomain = platformBaseDomain;
        this.skipCname = skipCname;
    }

    public String platformBaseDomain() {
        return platformBaseDomain;
    }

    public boolean skipCname() {
        return skipCname;
    }

    public String defaultHost(Shop shop) {
        return shop.getCode() + "." + platformBaseDomain;
    }

    public List<ShopDomain> listByShopId(Long shopId) {
        return shopDomainService.listByShopId(shopId);
    }

    @Transactional
    public ShopDomain applyCustom(Long shopId, String rawDomain) {
        String domain = normalize(rawDomain);
        if (!isValidDomain(domain)) {
            throw new BusinessException(ErrorCode.DOMAIN_INVALID);
        }
        Shop shop = requireShop(shopId);
        String target = defaultHost(shop);
        if (domain.equals(target)) {
            throw new BusinessException(ErrorCode.DOMAIN_INVALID, "请填写自己的域名，平台泛域名无需申请");
        }
        ShopDomain occupied = shopDomainService.findAnyByDomain(domain);
        if (occupied != null) {
            throw new BusinessException(ErrorCode.DOMAIN_ALREADY_BOUND);
        }
        boolean hasActiveCustom = shopDomainService.listByShopId(shopId).stream()
                .anyMatch(d -> "custom".equals(d.getType())
                        && ("pending".equals(d.getVerifyStatus()) || "verified".equals(d.getVerifyStatus())));
        if (hasActiveCustom) {
            throw new BusinessException(ErrorCode.DOMAIN_ALREADY_BOUND);
        }

        ShopDomain row = new ShopDomain();
        row.setShopId(shopId);
        row.setDomain(domain);
        row.setType("custom");
        row.setCertStatus("pending");
        row.setVerifyStatus("pending");
        row.setCnameTarget(target);
        row.setCnameStatus(skipCname || isLocalDevHost(domain) ? "skipped" : "pending");
        shopDomainService.save(row);
        return row;
    }

    @Transactional
    public ShopDomain checkCname(Long shopIdOrNull, Long id) {
        ShopDomain row = requireCustom(id, shopIdOrNull);
        if ("verified".equals(row.getVerifyStatus())) {
            return row;
        }
        String target = StringUtils.hasText(row.getCnameTarget())
                ? row.getCnameTarget()
                : defaultHost(requireShop(row.getShopId()));
        row.setCnameTarget(target);

        if (skipCname || isLocalDevHost(row.getDomain())) {
            row.setCnameStatus("skipped");
            shopDomainService.updateById(row);
            return row;
        }

        String actual = cnameLookup.lookupCname(row.getDomain())
                .map(ShopDomainBindingService::normalizeHost)
                .orElse("");
        boolean ok = !actual.isBlank() && actual.equals(normalizeHost(target));
        row.setCnameStatus(ok ? "ok" : "fail");
        shopDomainService.updateById(row);
        return row;
    }

    @Transactional
    public ShopDomain approve(Long id) {
        ShopDomain row = requireCustom(id, null);
        if (!"pending".equals(row.getVerifyStatus())) {
            throw new BusinessException(ErrorCode.DOMAIN_STATUS_INVALID, "仅待审核的自定义域名可通过");
        }
        if (!cnameReady(row)) {
            throw new BusinessException(ErrorCode.DOMAIN_CNAME_MISMATCH);
        }
        row.setVerifyStatus("verified");
        row.setCertStatus("pending");
        row.setCertExpireTime(null);
        row.setCertError(null);
        row.setRejectReason(null);
        shopDomainService.updateById(row);
        scheduleIssue(row);
        return row;
    }

    private void scheduleIssue(ShopDomain row) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Long id = row.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ShopDomain latest = shopDomainService.getOne(
                            Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getId, id));
                    sslCertificateService.issueAfterApprove(latest);
                }
            });
            return;
        }
        sslCertificateService.issueAfterApprove(row);
    }

    @Transactional
    public ShopDomain reject(Long id, String reason) {
        ShopDomain row = requireCustom(id, null);
        if (!"pending".equals(row.getVerifyStatus())) {
            throw new BusinessException(ErrorCode.DOMAIN_STATUS_INVALID, "仅待审核的自定义域名可驳回");
        }
        row.setVerifyStatus("rejected");
        row.setCertStatus("failed");
        row.setRejectReason(StringUtils.hasText(reason) ? reason.trim() : "平台驳回");
        shopDomainService.updateById(row);
        return row;
    }

    @Transactional
    public void unbind(Long shopIdOrNull, Long id) {
        ShopDomain row = requireCustom(id, shopIdOrNull);
        // uk_domain 不区分逻辑删除，先改名再删，避免解绑后无法重新申请同一域名
        row.setDomain("unbind." + row.getId() + ".invalid");
        row.setVerifyStatus("rejected");
        shopDomainService.updateById(row);
        shopDomainService.remove(Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getId, row.getId()));
    }

    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        int scheme = s.indexOf("://");
        if (scheme >= 0) {
            s = s.substring(scheme + 3);
        }
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        int colon = s.indexOf(':');
        if (colon >= 0) {
            s = s.substring(0, colon);
        }
        return normalizeHost(s);
    }

    static boolean isValidDomain(String domain) {
        return StringUtils.hasText(domain) && domain.length() <= 128 && DOMAIN_PATTERN.matcher(domain).matches();
    }

    private ShopDomain requireCustom(Long id, Long shopIdOrNull) {
        ShopDomain row = shopDomainService.getOne(Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getId, id));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "域名记录不存在");
        }
        if (shopIdOrNull != null && !shopIdOrNull.equals(row.getShopId())) {
            throw new TenantAccessDeniedException("id=" + id);
        }
        if (!"custom".equals(row.getType())) {
            throw new BusinessException(ErrorCode.DOMAIN_STATUS_INVALID, "平台泛域名不可进行此操作");
        }
        return row;
    }

    private Shop requireShop(Long shopId) {
        Shop shop = shopService.getOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getId, shopId));
        if (shop == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return shop;
    }

    private boolean cnameReady(ShopDomain row) {
        if (skipCname || isLocalDevHost(row.getDomain())) {
            return true;
        }
        return "ok".equals(row.getCnameStatus()) || "skipped".equals(row.getCnameStatus());
    }

    private static boolean isLocalDevHost(String domain) {
        return domain != null && (domain.equals("localhost") || domain.endsWith(".localhost"));
    }

    static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String s = host.trim().toLowerCase(Locale.ROOT);
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
