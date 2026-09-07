package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.domain.diy.entity.DiyPage;
import com.shopplatform.domain.diy.entity.DiyTemplate;
import com.shopplatform.domain.diy.mapper.DiyPageMapper;
import com.shopplatform.domain.diy.service.DiyPageService;
import com.shopplatform.domain.diy.service.DiyTemplateService;
import com.shopplatform.domain.diy.support.DiyPageContentValidator;
import com.shopplatform.domain.shop.service.PackageFeatureChecker;
import com.shopplatform.domain.shop.service.PackageQuotaChecker;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * {@link DiyPageServiceImpl} 单测：不拉 Spring 上下文，baseMapper 用反射注入 mock，
 * {@code getByIdWithTenant} 在 spy 上打桩——与 {@code OrderServiceImplTest}/{@code MemberServiceImplTest}
 * 同一套路。内容校验用真实 {@link DiyPageContentValidator}（配合 mock 的 {@link PackageFeatureChecker}），
 * 这样套餐锁定/未知组件类型两条分支能被真实走一遍，而不是靠 mock 掩盖。
 */
class DiyPageServiceImplTest {

    private static final Long SHOP_ID = 100L;

    private DiyPageMapper diyPageMapper;
    private DiyTemplateService diyTemplateService;
    private PackageFeatureChecker packageFeatureChecker;
    private DiyPageServiceImpl diyPageService;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DiyPage.class);
    }

    @BeforeEach
    void setUp() {
        diyPageMapper = mock(DiyPageMapper.class);
        diyTemplateService = mock(DiyTemplateService.class);
        packageFeatureChecker = mock(PackageFeatureChecker.class);
        DiyPageContentValidator validator = new DiyPageContentValidator(new ObjectMapper(), packageFeatureChecker);

        DiyPageServiceImpl impl = new DiyPageServiceImpl(diyTemplateService, validator, mock(PackageQuotaChecker.class));
        ReflectionTestUtils.setField(impl, "baseMapper", diyPageMapper);
        diyPageService = spy(impl);
        TenantContext.set(SHOP_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private DiyPage page(Long id, String pageType, Boolean isDefault, String pageData, String draftData) {
        DiyPage page = new DiyPage();
        page.setId(id);
        page.setShopId(SHOP_ID);
        page.setPageType(pageType);
        page.setName("测试页");
        page.setIsDefault(isDefault);
        page.setVersion(0);
        page.setPageData(pageData);
        page.setDraftData(draftData);
        return page;
    }

    private static final String VALID_CONTENT = "{\"page\":{},\"items\":[{\"type\":\"search\"},{\"type\":\"banner\"}]}";

    @Test
    void createPage_invalidPageType_throws() {
        assertThrows(BusinessException.class, () -> diyPageService.createPage(
                new DiyPageService.CreatePageCommand("新页", "category", null)));
    }

    @Test
    void createPage_copyFromPage_prefersPublishedSnapshotOverDraft() {
        DiyPage source = page(1L, "custom", false, "{\"published\":true}", "{\"draft\":true}");
        doReturn(source).when(diyPageService).getByIdWithTenant(1L);

        DiyPage created = diyPageService.createPage(new DiyPageService.CreatePageCommand("新页", "custom", 1L));

        assertEquals("{\"published\":true}", created.getDraftData());
        assertEquals(0, created.getVersion());
        assertFalse(created.getIsDefault());
    }

    @Test
    void createPage_copyFromUnpublishedPage_fallsBackToDraft() {
        DiyPage source = page(1L, "custom", false, null, "{\"draft\":true}");
        doReturn(source).when(diyPageService).getByIdWithTenant(1L);

        DiyPage created = diyPageService.createPage(new DiyPageService.CreatePageCommand("新页", "custom", 1L));

        assertEquals("{\"draft\":true}", created.getDraftData());
    }

    @Test
    void copyPage_usesPublishedSnapshotAndDefaultsNameWithSuffix() {
        DiyPage source = page(1L, "custom", false, "{\"published\":true}", "{\"draft\":true}");
        source.setName("原页");
        doReturn(source).when(diyPageService).getByIdWithTenant(1L);

        DiyPage copy = diyPageService.copyPage(1L, null);

        assertEquals("{\"published\":true}", copy.getDraftData());
        assertEquals("原页 副本", copy.getName());
        assertEquals(0, copy.getVersion());
        assertFalse(copy.getIsDefault());
    }

    @Test
    void copyPage_withExplicitName_usesGivenName() {
        DiyPage source = page(1L, "home", false, null, "{\"draft\":true}");
        doReturn(source).when(diyPageService).getByIdWithTenant(1L);

        DiyPage copy = diyPageService.copyPage(1L, "自定义名称");

        assertEquals("自定义名称", copy.getName());
        assertEquals("{\"draft\":true}", copy.getDraftData());
    }

    @Test
    void publish_validContent_incrementsVersionAndCopiesDraftToPageData() {
        DiyPage existing = page(1L, "custom", false, null, VALID_CONTENT);
        existing.setVersion(2);
        doReturn(existing).when(diyPageService).getByIdWithTenant(1L);

        DiyPage published = diyPageService.publish(1L);

        assertEquals(VALID_CONTENT, published.getPageData());
        assertEquals(3, published.getVersion());
        assertNotNull(published.getPublishTime());
    }

    @Test
    void publish_lockedMarketingComponent_throwsPackageFeatureLocked() {
        String content = "{\"page\":{},\"items\":[{\"type\":\"seckill\"}]}";
        DiyPage existing = page(1L, "custom", false, null, content);
        doReturn(existing).when(diyPageService).getByIdWithTenant(1L);
        when(packageFeatureChecker.hasMenu(SHOP_ID, "marketing.seckill")).thenReturn(false);
        doThrow(new BusinessException(com.shopplatform.common.result.ErrorCode.PACKAGE_FEATURE_LOCKED))
                .when(packageFeatureChecker).requireMenu(SHOP_ID, "marketing.seckill");

        assertThrows(BusinessException.class, () -> diyPageService.publish(1L));
    }

    @Test
    void publish_unknownComponentType_throwsDiyPageDataInvalid() {
        String content = "{\"page\":{},\"items\":[{\"type\":\"unknownWidget\"}]}";
        DiyPage existing = page(1L, "custom", false, null, content);
        doReturn(existing).when(diyPageService).getByIdWithTenant(1L);

        assertThrows(BusinessException.class, () -> diyPageService.publish(1L));
    }

    @Test
    void setHome_customPage_promotesToHome() {
        DiyPage custom = page(1L, "custom", false, null, VALID_CONTENT);
        doReturn(custom).when(diyPageService).getByIdWithTenant(1L);
        doReturn(true).when(diyPageService).update(any());

        diyPageService.setHome(1L);

        assertEquals("home", custom.getPageType());
        assertTrue(custom.getIsDefault());
        verify(diyPageService).update(any());
        verify(diyPageService).updateById(custom);
    }

    @Test
    void setHome_clearsOldDefaultThenSetsNewOne() {
        DiyPage target = page(2L, "home", false, VALID_CONTENT, VALID_CONTENT);
        doReturn(target).when(diyPageService).getByIdWithTenant(2L);
        doReturn(true).when(diyPageService).update(any());

        diyPageService.setHome(2L);

        verify(diyPageService).update(any());
        assertTrue(target.getIsDefault());
    }

    @Test
    void delete_currentHomePage_throws() {
        DiyPage home = page(1L, "home", true, VALID_CONTENT, VALID_CONTENT);
        doReturn(home).when(diyPageService).getByIdWithTenant(1L);

        assertThrows(BusinessException.class, () -> diyPageService.delete(1L));
        verify(diyPageService, never()).removeById(any(Long.class));
    }

    @Test
    void delete_nonHomePage_removes() {
        DiyPage custom = page(1L, "custom", false, VALID_CONTENT, VALID_CONTENT);
        doReturn(custom).when(diyPageService).getByIdWithTenant(1L);
        doReturn(true).when(diyPageService).removeById(1L);

        diyPageService.delete(1L);

        verify(diyPageService).removeById(1L);
    }

    @Test
    void applyTemplate_templateNotFound_throwsParamInvalid() {
        when(diyTemplateService.getByIdWithTenant(99L)).thenThrow(new TenantAccessDeniedException("id=99"));

        assertThrows(BusinessException.class, () -> diyPageService.applyTemplate(99L, "新页"));
    }

    @Test
    void applyTemplate_found_createsCustomDraftFromTemplateData() {
        DiyTemplate template = new DiyTemplate();
        template.setId(3L);
        template.setName("极简风");
        template.setPageData(VALID_CONTENT);
        when(diyTemplateService.getByIdWithTenant(3L)).thenReturn(template);

        DiyPage created = diyPageService.applyTemplate(3L, null);

        assertEquals("custom", created.getPageType());
        assertEquals(VALID_CONTENT, created.getDraftData());
        assertEquals("极简风", created.getName());
        assertEquals(0, created.getVersion());
    }

    @Test
    void getDefaultHome_exists_returnsPage() {
        DiyPage home = page(1L, "home", true, VALID_CONTENT, VALID_CONTENT);
        when(diyPageMapper.selectOne(any(), anyBoolean())).thenReturn(home);

        DiyPage result = diyPageService.getDefaultHome(SHOP_ID);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getDefaultHome_none_returnsNull() {
        when(diyPageMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        assertNull(diyPageService.getDefaultHome(SHOP_ID));
    }
}
