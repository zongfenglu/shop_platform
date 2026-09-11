package com.shopplatform.domain.file.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectStorageSupportTest {

    @Test
    void publicUrl_prefersConfiguredDomainAndAvoidsDoubleSlash() {
        assertEquals("https://cdn.example.com/assets/shop/1/a.jpg",
                ObjectStorageSupport.publicUrl("https://cdn.example.com/assets/",
                        "https://bucket.example.com", "shop/1/a.jpg"));
    }

    @Test
    void publicUrl_usesProviderDomainWhenCustomDomainIsBlank() {
        assertEquals("https://bucket.cos.ap-guangzhou.myqcloud.com/shop/1/a.jpg",
                ObjectStorageSupport.publicUrl(null,
                        "https://bucket.cos.ap-guangzhou.myqcloud.com", "shop/1/a.jpg"));
    }

    @Test
    void withoutScheme_normalizesEndpointForPublicUrl() {
        assertEquals("oss-cn-hangzhou.aliyuncs.com",
                ObjectStorageSupport.withoutScheme("https://oss-cn-hangzhou.aliyuncs.com/"));
    }
}
