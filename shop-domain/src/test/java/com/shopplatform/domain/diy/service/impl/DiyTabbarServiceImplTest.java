package com.shopplatform.domain.diy.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.diy.entity.DiyTabbar;
import com.shopplatform.domain.diy.mapper.DiyTabbarMapper;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiyTabbarServiceImplTest {

    private static final Long SHOP_ID = 100L;

    private DiyTabbarMapper diyTabbarMapper;
    private DiyTabbarServiceImpl diyTabbarService;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DiyTabbar.class);
    }

    @BeforeEach
    void setUp() {
        diyTabbarMapper = mock(DiyTabbarMapper.class);
        DiyTabbarServiceImpl impl = new DiyTabbarServiceImpl(new ObjectMapper());
        ReflectionTestUtils.setField(impl, "baseMapper", diyTabbarMapper);
        diyTabbarService = spy(impl);
        TenantContext.set(SHOP_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getOrCreateDefault_noExistingRow_createsFourDefaultItems() {
        doReturn(null).when(diyTabbarService).getOne(any());
        doReturn(true).when(diyTabbarService).save(any());

        DiyTabbar tabbar = diyTabbarService.getOrCreateDefault();

        assertEquals(SHOP_ID, tabbar.getShopId());
        assertTrue(tabbar.getItems().contains("首页"));
        verify(diyTabbarService).save(tabbar);
    }

    @Test
    void getOrCreateDefault_existingRow_returnsItWithoutCreating() {
        DiyTabbar existing = new DiyTabbar();
        existing.setId(1L);
        existing.setShopId(SHOP_ID);
        existing.setItems("[]");
        doReturn(existing).when(diyTabbarService).getOne(any());

        DiyTabbar result = diyTabbarService.getOrCreateDefault();

        assertSame(existing, result);
        verify(diyTabbarService, never()).save(any());
    }

    @Test
    void save_tooFewItems_throwsItemCountInvalid() {
        assertThrows(BusinessException.class, () -> diyTabbarService.save("[{\"text\":\"首页\"}]", null));
    }

    @Test
    void save_tooManyItems_throwsItemCountInvalid() {
        String sixItems = "[{},{},{},{},{},{}]";
        assertThrows(BusinessException.class, () -> diyTabbarService.save(sixItems, null));
    }

    @Test
    void save_validCount_updatesExistingRow() {
        DiyTabbar existing = new DiyTabbar();
        existing.setId(1L);
        existing.setShopId(SHOP_ID);
        existing.setItems("[]");
        doReturn(existing).when(diyTabbarService).getOne(any());
        doReturn(true).when(diyTabbarService).updateById(any());

        String items = "[{\"text\":\"首页\"},{\"text\":\"我的\"}]";
        DiyTabbar result = diyTabbarService.save(items, "{\"activeColor\":\"#f00\"}");

        assertEquals(items, result.getItems());
        assertEquals("{\"activeColor\":\"#f00\"}", result.getStyle());
        verify(diyTabbarService).updateById(existing);
    }
}
