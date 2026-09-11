package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.goods.entity.GoodsCategory;
import com.shopplatform.domain.goods.service.GoodsCategoryService;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoodsCategoryServiceImplTest {

    private GoodsService goodsService;
    private GoodsCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContext.set(1001L);
        goodsService = mock(GoodsService.class);
        service = spy(new GoodsCategoryServiceImpl(goodsService));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_root_setsShopAndShow() {
        doReturn(true).when(service).save(any(GoodsCategory.class));

        GoodsCategory created = service.create(new GoodsCategoryService.SaveCommand(0L, " 女装 ", null, 1, null));

        assertEquals("女装", created.getName());
        assertEquals(0L, created.getParentId());
        assertEquals(1001L, created.getShopId());
        assertTrue(created.getIsShow());
        verify(service).save(created);
    }

    @Test
    void create_rejectsFourthLevel() {
        GoodsCategory l1 = cat(1L, 0L);
        GoodsCategory l2 = cat(2L, 1L);
        GoodsCategory l3 = cat(3L, 2L);
        doReturn(l3).when(service).getById(3L);
        doReturn(l3).when(service).findParent(3L);
        doReturn(l2).when(service).findParent(2L);
        doReturn(l1).when(service).findParent(1L);

        assertThrows(BusinessException.class,
                () -> service.create(new GoodsCategoryService.SaveCommand(3L, "不能再分", null, 0, true)));
        verify(service, never()).save(any());
    }

    @Test
    void update_keepsParent() {
        GoodsCategory existing = cat(10L, 0L);
        existing.setName("旧名");
        doReturn(existing).when(service).getById(10L);
        doReturn(true).when(service).updateById(existing);

        service.update(10L, new GoodsCategoryService.SaveCommand(99L, "新名", "/img.png", 8, false));

        assertEquals("新名", existing.getName());
        assertEquals("/img.png", existing.getImage());
        assertEquals(8, existing.getSort());
        assertEquals(Boolean.FALSE, existing.getIsShow());
        assertEquals(0L, existing.getParentId());
    }

    @Test
    void listSelfAndDescendantIds_returnsWholeSubtree() {
        doReturn(List.of(
                cat(1L, 0L),
                cat(2L, 1L),
                cat(3L, 2L),
                cat(4L, 0L)
        )).when(service).list();

        assertEquals(List.of(1L, 2L, 3L), service.listSelfAndDescendantIds(1L));
        assertEquals(List.of(2L, 3L), service.listSelfAndDescendantIds(2L));
        assertEquals(List.of(), service.listSelfAndDescendantIds(99L));
    }

    @Test
    void delete_rejectsWhenHasChildren() {
        doReturn(cat(10L, 0L)).when(service).getById(10L);
        doReturn(2L).when(service).count(any(Wrapper.class));

        assertThrows(BusinessException.class, () -> service.deleteCategory(10L));
        verify(goodsService, never()).count(any());
        verify(service, never()).removeById(eq(10L));
    }

    @Test
    void delete_rejectsWhenGoodsUseIt() {
        doReturn(cat(10L, 0L)).when(service).getById(10L);
        doReturn(0L).when(service).count(any(Wrapper.class));
        when(goodsService.count(any(Wrapper.class))).thenReturn(3L);

        assertThrows(BusinessException.class, () -> service.deleteCategory(10L));
        verify(service, never()).removeById(eq(10L));
    }

    @Test
    void delete_okWhenUnused() {
        doReturn(cat(10L, 0L)).when(service).getById(10L);
        doReturn(0L).when(service).count(any(Wrapper.class));
        when(goodsService.count(any(Wrapper.class))).thenReturn(0L);
        doReturn(true).when(service).removeById(10L);

        service.deleteCategory(10L);

        verify(service).removeById(10L);
    }

    private static GoodsCategory cat(long id, long parentId) {
        GoodsCategory c = new GoodsCategory();
        c.setId(id);
        c.setParentId(parentId);
        c.setName("c" + id);
        c.setIsShow(true);
        return c;
    }
}
