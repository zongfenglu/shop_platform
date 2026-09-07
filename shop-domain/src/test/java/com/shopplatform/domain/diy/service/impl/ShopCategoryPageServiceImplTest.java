package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.diy.entity.ShopCategoryPage;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ShopCategoryPageServiceImplTest {

    private ShopCategoryPageServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContext.set(1001L);
        service = spy(new ShopCategoryPageServiceImpl());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getOrCreate_writesDefaultSmallGrid() {
        doReturn(null).when(service).getOne(any(Wrapper.class));
        doReturn(true).when(service).save(any(ShopCategoryPage.class));

        ShopCategoryPage row = service.getOrCreate();

        assertEquals(ShopCategoryPage.STYLE_LEVEL1_SMALL, row.getStyle());
        assertEquals("全部分类", row.getShareTitle());
        assertEquals(1001L, row.getShopId());
        verify(service).save(row);
    }

    @Test
    void saveStyle_rejectsUnknown() {
        assertThrows(BusinessException.class, () -> service.saveStyle("unknown", "欢迎采购"));
    }

    @Test
    void saveStyle_updatesExisting() {
        ShopCategoryPage existing = new ShopCategoryPage();
        existing.setId(1L);
        existing.setStyle(ShopCategoryPage.STYLE_LEVEL1_SMALL);
        doReturn(existing).when(service).getOne(any(Wrapper.class));
        doReturn(true).when(service).updateById(existing);

        ShopCategoryPage saved = service.saveStyle(ShopCategoryPage.STYLE_LEVEL2, "欢迎进行设备采购!");

        assertEquals(ShopCategoryPage.STYLE_LEVEL2, saved.getStyle());
        assertEquals("欢迎进行设备采购!", saved.getShareTitle());
        verify(service).updateById(existing);
    }
}
