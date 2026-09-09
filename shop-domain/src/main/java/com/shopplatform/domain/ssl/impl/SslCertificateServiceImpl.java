package com.shopplatform.domain.ssl.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.ssl.AcmeIssuer;
import com.shopplatform.domain.ssl.AcmeProperties;
import com.shopplatform.domain.ssl.SslCertificateService;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SslCertificateServiceImpl implements SslCertificateService {

    private static final Logger log = LoggerFactory.getLogger(SslCertificateServiceImpl.class);

    private final ShopDomainService shopDomainService;
    private final AcmeIssuer acmeIssuer;
    private final RedisHttp01Store http01Store;
    private final AcmeProperties properties;
    private final AesGcmEncryptor aesGcmEncryptor;

    public SslCertificateServiceImpl(ShopDomainService shopDomainService,
                                     AcmeIssuer acmeIssuer,
                                     RedisHttp01Store http01Store,
                                     AcmeProperties properties,
                                     AesGcmEncryptor aesGcmEncryptor) {
        this.shopDomainService = shopDomainService;
        this.acmeIssuer = acmeIssuer;
        this.http01Store = http01Store;
        this.properties = properties;
        this.aesGcmEncryptor = aesGcmEncryptor;
    }

    @Override
    public void issueAfterApprove(ShopDomain domain) {
        if (domain == null) {
            return;
        }
        if (isLocalDevHost(domain.getDomain())) {
            persist(domain, "pending", null, "本地/内网域名不能向 Let's Encrypt 申请证书");
            return;
        }
        if (!properties.ready()) {
            persist(domain, "pending", null, disabledReason());
            return;
        }
        try {
            doIssue(domain);
        } catch (RuntimeException e) {
            persist(domain, "failed", null, trim(e.getMessage()));
        }
    }

    @Override
    public ShopDomain issueNow(Long domainId) {
        ShopDomain row = requireVerifiedCustom(domainId);
        if (isLocalDevHost(row.getDomain())) {
            persist(row, "pending", null, "本地/内网域名不能向 Let's Encrypt 申请证书");
            throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "本地/内网域名不能向 Let's Encrypt 申请证书");
        }
        if (!properties.ready()) {
            persist(row, "pending", null, disabledReason());
            throw new BusinessException(ErrorCode.ACME_NOT_ENABLED, disabledReason());
        }
        try {
            return doIssue(row);
        } catch (BusinessException e) {
            persist(row, "failed", null, trim(e.getMessage()));
            throw e;
        } catch (RuntimeException e) {
            persist(row, "failed", null, trim(e.getMessage()));
            throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, trim(e.getMessage()));
        }
    }

    @Override
    public ShopDomain upload(Long domainId, MultipartFile certificate, MultipartFile privateKey) {
        ShopDomain row = requireVerifiedCustom(domainId);
        if (certificate == null || privateKey == null || certificate.isEmpty() || privateKey.isEmpty())
            throw new BusinessException(ErrorCode.PARAM_INVALID, "证书和私钥不能为空");
        if (certificate.getSize() > 2_000_000 || privateKey.getSize() > 2_000_000)
            throw new BusinessException(ErrorCode.PARAM_INVALID, "证书文件不能超过 2MB");
        try {
            String certPem = new String(certificate.getBytes(), StandardCharsets.UTF_8);
            String keyPem = new String(privateKey.getBytes(), StandardCharsets.UTF_8);
            if (!certPem.contains("BEGIN CERTIFICATE") || !keyPem.contains("BEGIN")) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "请上传 PEM 格式证书和私钥");
            }
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate x509 = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));
            LocalDateTime expire = x509.getNotAfter().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            if (expire.isBefore(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "证书已过期");
            }
            row.setCertPemEncrypted(aesGcmEncryptor.encrypt(certPem));
            row.setKeyPemEncrypted(aesGcmEncryptor.encrypt(keyPem));
            persist(row, "valid", expire, null);
            writePemFiles(row.getDomain(), certPem, keyPem);
            reloadGateway();
            return row;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "证书解析失败: " + trim(e.getMessage()));
        }
    }

    @Override
    public int renewDue() {
        LocalDateTime now = LocalDateTime.now();
        List<ShopDomain> expired = shopDomainService.list(Wrappers.<ShopDomain>lambdaQuery()
                .eq(ShopDomain::getType, "custom")
                .eq(ShopDomain::getVerifyStatus, "verified")
                .eq(ShopDomain::getCertStatus, "valid")
                .isNotNull(ShopDomain::getCertExpireTime)
                .lt(ShopDomain::getCertExpireTime, now));
        for (ShopDomain row : expired) {
            persist(row, "expired", row.getCertExpireTime(), "证书已过期");
        }

        if (!properties.ready()) {
            return 0;
        }
        LocalDateTime soon = now.plusDays(30);
        List<ShopDomain> due = shopDomainService.list(Wrappers.<ShopDomain>lambdaQuery()
                .eq(ShopDomain::getType, "custom")
                .eq(ShopDomain::getVerifyStatus, "verified")
                .in(ShopDomain::getCertStatus, List.of("valid", "expired", "failed", "pending"))
                .and(w -> w.isNull(ShopDomain::getCertExpireTime)
                        .or().lt(ShopDomain::getCertExpireTime, soon)));
        int ok = 0;
        for (ShopDomain row : due) {
            if (isLocalDevHost(row.getDomain())) {
                continue;
            }
            try {
                doIssue(row);
                ok++;
            } catch (RuntimeException e) {
                persist(row, "failed", row.getCertExpireTime(), trim(e.getMessage()));
                log.warn("证书续期失败 domain={}: {}", row.getDomain(), e.getMessage());
            }
        }
        return ok;
    }

    @Override
    public Optional<String> http01Authorization(String token) {
        return http01Store.find(token);
    }

    private ShopDomain doIssue(ShopDomain domain) {
        persist(domain, "issuing", null, null);
        AcmeIssuer.IssuedCert issued = acmeIssuer.issue(domain.getDomain(), http01Store);
        domain.setCertPemEncrypted(aesGcmEncryptor.encrypt(issued.certPem()));
        domain.setKeyPemEncrypted(aesGcmEncryptor.encrypt(issued.keyPem()));
        persist(domain, "valid", issued.notAfter(), null);
        writePemFiles(domain.getDomain(), issued.certPem(), issued.keyPem());
        reloadGateway();
        return domain;
    }

    private void writePemFiles(String domain, String certPem, String keyPem) {
        try {
            Path dir = Path.of(properties.certDir());
            Files.createDirectories(dir);
            Path crt = dir.resolve(domain + ".crt");
            Path key = dir.resolve(domain + ".key");
            Files.writeString(crt, certPem, StandardCharsets.UTF_8);
            Files.writeString(key, keyPem, StandardCharsets.UTF_8);
            try {
                Set<PosixFilePermission> priv = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(key, priv);
            } catch (UnsupportedOperationException ignored) {
                // Windows 开发机没有 POSIX 权限
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "证书已签发但写入磁盘失败: " + trim(e.getMessage()));
        }
    }

    private void reloadGateway() {
        String cmd = properties.reloadCommand();
        if (!StringUtils.hasText(cmd)) {
            log.info("证书已写入 {}，未配置 shop.acme.reload-command，跳过网关热加载", properties.certDir());
            return;
        }
        try {
            Process process = new ProcessBuilder(cmd.split("\\s+"))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "网关重载命令超时");
            }
            if (process.exitValue() != 0) {
                throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "网关重载失败 exit=" + process.exitValue());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ACME_ISSUE_FAILED, "网关重载失败: " + trim(e.getMessage()));
        }
    }

    private ShopDomain requireVerifiedCustom(Long id) {
        ShopDomain row = shopDomainService.getOne(Wrappers.<ShopDomain>lambdaQuery().eq(ShopDomain::getId, id));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "域名记录不存在");
        }
        if (!"custom".equals(row.getType()) || !"verified".equals(row.getVerifyStatus())) {
            throw new BusinessException(ErrorCode.DOMAIN_STATUS_INVALID, "仅已审核通过的自定义域名可签发证书");
        }
        return row;
    }

    private void persist(ShopDomain domain, String certStatus, LocalDateTime expire, String error) {
        domain.setCertStatus(certStatus);
        domain.setCertExpireTime(expire);
        domain.setCertError(error);
        shopDomainService.updateById(domain);
    }

    private String disabledReason() {
        if (!properties.enabled()) {
            return "Let's Encrypt 未启用（shop.acme.enabled=false）。审核已通过，证书不会假装签发。";
        }
        return "Let's Encrypt 已打开但缺少 directory-url 或 email";
    }

    private static boolean isLocalDevHost(String domain) {
        return domain != null && (domain.equals("localhost")
                || domain.endsWith(".localhost")
                || domain.endsWith(".local")
                || domain.endsWith(".invalid"));
    }

    private static String trim(String msg) {
        if (msg == null || msg.isBlank()) {
            return "证书签发失败";
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
