package com.shopplatform.domain.ssl.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.ssl.AcmeIssuer;
import com.shopplatform.domain.ssl.AcmeProperties;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SslCertificateServiceImplTest {

    private ShopDomainService shopDomainService;
    private AcmeIssuer acmeIssuer;
    private RedisHttp01Store http01Store;
    private AesGcmEncryptor aes;
    private Path certDir;

    @BeforeEach
    void setUp(@TempDir Path dir) {
        shopDomainService = mock(ShopDomainService.class);
        acmeIssuer = mock(AcmeIssuer.class);
        http01Store = mock(RedisHttp01Store.class);
        aes = new AesGcmEncryptor("test-aes-key-for-ssl-unit");
        certDir = dir;
    }

    @Test
    void issueAfterApprove_doesNotFakeValidWhenDisabled() {
        SslCertificateServiceImpl svc = service(false, "");
        ShopDomain row = custom("mall.example.com");
        svc.issueAfterApprove(row);
        assertEquals("pending", row.getCertStatus());
        assertNull(row.getCertExpireTime());
        assertTrue(row.getCertError().contains("未启用"));
        verify(acmeIssuer, never()).issue(any(), any());
    }

    @Test
    void issueAfterApprove_skipsLocalhost() {
        SslCertificateServiceImpl svc = service(true, "ops@example.com");
        ShopDomain row = custom("demo.localhost");
        svc.issueAfterApprove(row);
        assertEquals("pending", row.getCertStatus());
        assertTrue(row.getCertError().contains("本地"));
        verify(acmeIssuer, never()).issue(any(), any());
    }

    @Test
    void issueNow_writesPemWhenIssuerSucceeds() throws Exception {
        SslCertificateServiceImpl svc = service(true, "ops@example.com");
        ShopDomain row = custom("mall.example.com");
        row.setVerifyStatus("verified");
        when(shopDomainService.getOne(any())).thenReturn(row);
        when(acmeIssuer.issue(eq("mall.example.com"), any())).thenReturn(
                new AcmeIssuer.IssuedCert("CERT", "KEY", LocalDateTime.of(2027, 1, 1, 0, 0), "LE"));

        ShopDomain issued = svc.issueNow(9L);
        assertEquals("valid", issued.getCertStatus());
        assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), issued.getCertExpireTime());
        assertNull(issued.getCertError());
        assertTrue(aes.decrypt(issued.getCertPemEncrypted()).contains("CERT"));
        assertTrue(java.nio.file.Files.readString(certDir.resolve("mall.example.com.crt")).contains("CERT"));
    }

    @Test
    void issueNow_throwsWhenDisabled() {
        SslCertificateServiceImpl svc = service(false, "");
        ShopDomain row = custom("mall.example.com");
        row.setVerifyStatus("verified");
        when(shopDomainService.getOne(any())).thenReturn(row);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.issueNow(9L));
        assertEquals(ErrorCode.ACME_NOT_ENABLED.getCode(), ex.getCode());
    }

    @Test
    void http01Authorization_readsStore() {
        SslCertificateServiceImpl svc = service(false, "");
        when(http01Store.find("tok")).thenReturn(Optional.of("keyauth"));
        assertEquals("keyauth", svc.http01Authorization("tok").orElseThrow());
    }

    private SslCertificateServiceImpl service(boolean enabled, String email) {
        AcmeProperties props = new AcmeProperties(
                enabled,
                "https://acme-staging-v02.api.letsencrypt.org/directory",
                email,
                certDir.toString(),
                "");
        return new SslCertificateServiceImpl(shopDomainService, acmeIssuer, http01Store, props, aes);
    }

    private static ShopDomain custom(String domain) {
        ShopDomain row = new ShopDomain();
        row.setId(9L);
        row.setShopId(1001L);
        row.setDomain(domain);
        row.setType("custom");
        row.setVerifyStatus("verified");
        row.setCertStatus("pending");
        return row;
    }
}
