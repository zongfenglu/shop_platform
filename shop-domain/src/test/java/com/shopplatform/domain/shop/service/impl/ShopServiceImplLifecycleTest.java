package com.shopplatform.domain.shop.service.impl;

import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.member.service.UserGradeService;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.service.PackageTplService;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.StoreRoleService;
import com.shopplatform.domain.shop.service.StoreUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopServiceImplLifecycleTest {

    private StoreUserService storeUserService;
    private PasswordEncoder passwordEncoder;
    private ShopServiceImpl service;

    @BeforeEach
    void setUp() {
        storeUserService = mock(StoreUserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("newpass12")).thenReturn("hashed");
        service = spy(new ShopServiceImpl(
                mock(PackageTplService.class),
                mock(ShopPackageService.class),
                mock(StoreRoleService.class),
                storeUserService,
                mock(ShopDomainService.class),
                mock(UserGradeService.class),
                passwordEncoder));
    }

    @Test
    void disable_setsDisabled() {
        Shop shop = shop(1001L, ShopStatus.NORMAL.getCode());
        doReturn(shop).when(service).getByIdWithTenant(1001L);
        doReturn(true).when(service).updateById(shop);

        service.disable(1001L);

        assertEquals(ShopStatus.DISABLED.getCode(), shop.getStatus());
        verify(service).updateById(shop);
    }

    @Test
    void enable_restoresExpiredWhenPastExpireTime() {
        Shop shop = shop(1001L, ShopStatus.DISABLED.getCode());
        shop.setExpireTime(LocalDateTime.now().minusDays(1));
        doReturn(shop).when(service).getByIdWithTenant(1001L);
        doReturn(true).when(service).updateById(shop);

        service.enable(1001L);

        assertEquals(ShopStatus.EXPIRED.getCode(), shop.getStatus());
    }

    @Test
    void enable_restoresNormalWhenStillValid() {
        Shop shop = shop(1001L, ShopStatus.DISABLED.getCode());
        shop.setExpireTime(LocalDateTime.now().plusDays(30));
        doReturn(shop).when(service).getByIdWithTenant(1001L);
        doReturn(true).when(service).updateById(shop);

        service.enable(1001L);

        assertEquals(ShopStatus.NORMAL.getCode(), shop.getStatus());
    }

    @Test
    void enable_rejectsNonDisabled() {
        Shop shop = shop(1001L, ShopStatus.NORMAL.getCode());
        doReturn(shop).when(service).getByIdWithTenant(1001L);
        assertThrows(BusinessException.class, () -> service.enable(1001L));
    }

    @Test
    void resetOwnerPassword_updatesSuperOwner() {
        Shop shop = shop(1001L, ShopStatus.NORMAL.getCode());
        doReturn(shop).when(service).getByIdWithTenant(1001L);
        StoreUser owner = new StoreUser();
        owner.setId(9L);
        owner.setIsSuperOwner(true);
        when(storeUserService.findSuperOwner(1001L)).thenReturn(owner);

        service.resetOwnerPassword(1001L, "newpass12");

        assertEquals("hashed", owner.getPassword());
        verify(storeUserService).updateById(owner);
    }

    private static Shop shop(Long id, String status) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("demo");
        shop.setStatus(status);
        return shop;
    }
}
