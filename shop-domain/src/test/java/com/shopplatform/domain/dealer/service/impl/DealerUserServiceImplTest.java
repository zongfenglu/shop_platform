package com.shopplatform.domain.dealer.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class DealerUserServiceImplTest {

    private DealerUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new DealerUserServiceImpl(mock(DealerSettingService.class)));
        doReturn(true).when(service).updateById(org.mockito.ArgumentMatchers.any(DealerUser.class));
    }

    @Test
    void enable_disabledDealer_restoresActiveStatus() {
        DealerUser dealer = new DealerUser();
        dealer.setId(10L);
        dealer.setStatus("disabled");
        doReturn(dealer).when(service).getByIdWithTenant(10L);

        DealerUser result = service.enable(10L);

        assertEquals("active", result.getStatus());
        verify(service).updateById(dealer);
    }

    @Test
    void enable_activeDealer_rejectsInvalidTransition() {
        DealerUser dealer = new DealerUser();
        dealer.setId(10L);
        dealer.setStatus("active");
        doReturn(dealer).when(service).getByIdWithTenant(10L);

        assertThrows(BusinessException.class, () -> service.enable(10L));
    }
}
