package com.shopplatform.domain.diy.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.diy.entity.DiyMaterial;
import com.shopplatform.domain.diy.service.DiyMaterialGroupService;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class DiyMaterialServiceImplTest {
    private DiyMaterialServiceImpl service;
    private DiyMaterial material;

    @BeforeEach
    void setUp() {
        TenantContext.set(1001L);
        service = spy(new DiyMaterialServiceImpl(mock(DiyMaterialGroupService.class)));
        material = new DiyMaterial();
        material.setId(8L);
        material.setRecycled(false);
        doReturn(material).when(service).getById(8L);
        doReturn(true).when(service).updateById(material);
    }

    @AfterEach void tearDown() { TenantContext.clear(); }

    @Test
    void removeAndRestorePreserveRecord() {
        service.remove(8L);
        assertTrue(material.getRecycled());
        assertNotNull(material.getRecycleTime());
        service.restore(8L);
        assertFalse(material.getRecycled());
    }

    @Test
    void permanentRemoveRequiresRecycleBin() {
        assertThrows(BusinessException.class, () -> service.permanentlyRemove(8L));
        material.setRecycled(true);
        doReturn(true).when(service).removeById(8L);
        service.permanentlyRemove(8L);
        verify(service).removeById(8L);
    }
}
