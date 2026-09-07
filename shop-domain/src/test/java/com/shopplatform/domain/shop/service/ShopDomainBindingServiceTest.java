package com.shopplatform.domain.shop.service;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.dns.CnameLookup;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.ssl.SslCertificateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopDomainBindingServiceTest {

    private ShopDomainService shopDomainService;
    private ShopService shopService;
    private CnameLookup cnameLookup;
    private SslCertificateService sslCertificateService;
    private ShopDomainBindingService strict;
    private ShopDomainBindingService localSkip;

    @BeforeEach
    void setUp() {
        shopDomainService = mock(ShopDomainService.class);
        shopService = mock(ShopService.class);
        cnameLookup = mock(CnameLookup.class);
        sslCertificateService = mock(SslCertificateService.class);
        strict = new ShopDomainBindingService(shopDomainService, shopService, cnameLookup, sslCertificateService, "shop.com", false);
        localSkip = new ShopDomainBindingService(shopDomainService, shopService, cnameLookup, sslCertificateService, "shop.com", true);
    }

    @Test
    void normalize_stripsSchemePortAndPath() {
        assertEquals("www.example.com", ShopDomainBindingService.normalize("HTTPS://WWW.Example.com:443/h5"));
        assertTrue(ShopDomainBindingService.isValidDomain("www.example.com"));
        assertTrue(ShopDomainBindingService.isValidDomain("demo.localhost"));
        assertEquals(false, ShopDomainBindingService.isValidDomain("nope"));
    }

    @Test
    void applyCustom_rejectsInvalidAndDuplicate() {
        Shop shop = new Shop();
        shop.setId(1001L);
        shop.setCode("demo");
        when(shopService.getOne(any())).thenReturn(shop);
        when(shopDomainService.listByShopId(1001L)).thenReturn(List.of());
        when(shopDomainService.findAnyByDomain("www.example.com")).thenReturn(null);

        assertThrows(BusinessException.class, () -> strict.applyCustom(1001L, "not a host"));

        ShopDomain occupied = new ShopDomain();
        occupied.setId(1L);
        when(shopDomainService.findAnyByDomain("taken.example.com")).thenReturn(occupied);
        BusinessException dup = assertThrows(BusinessException.class, () -> strict.applyCustom(1001L, "taken.example.com"));
        assertEquals(ErrorCode.DOMAIN_ALREADY_BOUND.getCode(), dup.getCode());
    }

    @Test
    void applyCustom_savesPendingRow() {
        Shop shop = new Shop();
        shop.setId(1001L);
        shop.setCode("demo");
        when(shopService.getOne(any())).thenReturn(shop);
        when(shopDomainService.listByShopId(1001L)).thenReturn(List.of());
        when(shopDomainService.findAnyByDomain("mall.example.com")).thenReturn(null);

        ShopDomain saved = strict.applyCustom(1001L, "mall.example.com");
        assertEquals("mall.example.com", saved.getDomain());
        assertEquals("custom", saved.getType());
        assertEquals("pending", saved.getVerifyStatus());
        assertEquals("demo.shop.com", saved.getCnameTarget());
        assertEquals("pending", saved.getCnameStatus());
        verify(shopDomainService).save(any(ShopDomain.class));
    }

    @Test
    void checkCname_marksOkWhenLookupMatchesTarget() {
        ShopDomain row = pendingCustom();
        when(shopDomainService.getOne(any())).thenReturn(row);
        when(cnameLookup.lookupCname("mall.example.com")).thenReturn(Optional.of("demo.shop.com."));

        ShopDomain updated = strict.checkCname(1001L, 9L);
        assertEquals("ok", updated.getCnameStatus());
    }

    @Test
    void approve_requiresCnameUnlessSkipped() {
        ShopDomain row = pendingCustom();
        row.setCnameStatus("pending");
        when(shopDomainService.getOne(any())).thenReturn(row);

        BusinessException ex = assertThrows(BusinessException.class, () -> strict.approve(9L));
        assertEquals(ErrorCode.DOMAIN_CNAME_MISMATCH.getCode(), ex.getCode());

        row.setCnameStatus("ok");
        ShopDomain approved = strict.approve(9L);
        assertEquals("verified", approved.getVerifyStatus());
        assertEquals("pending", approved.getCertStatus());
        verify(sslCertificateService).issueAfterApprove(approved);
    }

    @Test
    void approve_allowsSkipCnameFlag() {
        ShopDomain row = pendingCustom();
        row.setCnameStatus("pending");
        when(shopDomainService.getOne(any())).thenReturn(row);
        ShopDomain approved = localSkip.approve(9L);
        assertEquals("verified", approved.getVerifyStatus());
    }

    private static ShopDomain pendingCustom() {
        ShopDomain row = new ShopDomain();
        row.setId(9L);
        row.setShopId(1001L);
        row.setDomain("mall.example.com");
        row.setType("custom");
        row.setVerifyStatus("pending");
        row.setCnameTarget("demo.shop.com");
        row.setCnameStatus("pending");
        return row;
    }
}
