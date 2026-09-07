package com.shopplatform.domain.offlinestore.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.domain.offlinestore.service.VerifyLogService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.shop.entity.StoreRole;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.service.StoreRoleService;
import com.shopplatform.domain.shop.service.StoreUserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PickupVerifyServiceImplTest {

    private static final Long CLERK_USER_ID = 501L;
    private static final Long STORE_A_ID = 10L;
    private static final Long STORE_B_ID = 20L;

    private OrderService orderService;
    private StoreUserService storeUserService;
    private StoreRoleService storeRoleService;
    private VerifyLogService verifyLogService;
    private PickupVerifyServiceImpl pickupVerifyService;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Order.class);
    }

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        storeUserService = mock(StoreUserService.class);
        storeRoleService = mock(StoreRoleService.class);
        verifyLogService = mock(VerifyLogService.class);
        pickupVerifyService = new PickupVerifyServiceImpl(orderService, storeUserService, storeRoleService, verifyLogService);
    }

    private Order pickupOrder(String payStatus, String deliveryStatus, Long pickupStoreId) {
        Order order = new Order();
        order.setId(9001L);
        order.setDeliveryType("pickup");
        order.setPickupCode("123456");
        order.setPayStatus(payStatus);
        order.setDeliveryStatus(deliveryStatus);
        order.setPickupStoreId(pickupStoreId);
        return order;
    }

    private StoreUser owner() {
        StoreUser user = new StoreUser();
        user.setId(CLERK_USER_ID);
        user.setRoleId(null);
        return user;
    }

    private StoreUser clerk(Long roleId, Long storeOfflineId) {
        StoreUser user = new StoreUser();
        user.setId(CLERK_USER_ID);
        user.setRoleId(roleId);
        user.setStoreOfflineId(storeOfflineId);
        return user;
    }

    private StoreRole storeScopeRole() {
        StoreRole role = new StoreRole();
        role.setId(1L);
        role.setDataScope("store");
        return role;
    }

    @Test
    void verify_blankCode_throwsParamInvalid() {
        assertThrows(BusinessException.class, () -> pickupVerifyService.verify("  ", CLERK_USER_ID));
        verifyNoInteractions(orderService);
    }

    @Test
    void verify_codeNotFound_throwsVerifyCodeInvalid() {
        when(orderService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> pickupVerifyService.verify("123456", CLERK_USER_ID));
    }

    @Test
    void verify_unpaidOrder_throwsOrderStatusInvalid() {
        Order order = pickupOrder("unpaid", "pending", STORE_A_ID);
        when(orderService.getOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertThrows(BusinessException.class, () -> pickupVerifyService.verify("123456", CLERK_USER_ID));
        verify(orderService, never()).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    void verify_ownerRole_anyStoreAllowed_succeedsAndLogs() {
        Order order = pickupOrder("paid", "pending", STORE_A_ID);
        when(orderService.getOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(storeUserService.getByIdWithTenant(CLERK_USER_ID)).thenReturn(owner());
        when(orderService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        Order result = pickupVerifyService.verify("123456", CLERK_USER_ID);

        assertEquals("received", result.getDeliveryStatus());
        assertEquals("finished", result.getOrderStatus());
        verify(verifyLogService).save(argThat(log -> log.getOrderId().equals(order.getId())
                && log.getStoreId().equals(STORE_A_ID) && log.getClerkId().equals(CLERK_USER_ID)));
    }

    @Test
    void verify_clerkWrongStore_throwsTenantAccessDenied() {
        Order order = pickupOrder("paid", "pending", STORE_B_ID);
        when(orderService.getOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(storeUserService.getByIdWithTenant(CLERK_USER_ID)).thenReturn(clerk(2L, STORE_A_ID));
        when(storeRoleService.getByIdWithTenant(2L)).thenReturn(storeScopeRole());

        assertThrows(TenantAccessDeniedException.class, () -> pickupVerifyService.verify("123456", CLERK_USER_ID));
        verify(orderService, never()).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    void verify_clerkOwnStore_succeeds() {
        Order order = pickupOrder("paid", "pending", STORE_A_ID);
        when(orderService.getOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(storeUserService.getByIdWithTenant(CLERK_USER_ID)).thenReturn(clerk(2L, STORE_A_ID));
        when(storeRoleService.getByIdWithTenant(2L)).thenReturn(storeScopeRole());
        when(orderService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        Order result = pickupVerifyService.verify("123456", CLERK_USER_ID);

        assertEquals("finished", result.getOrderStatus());
    }

    @Test
    void verify_alreadyVerified_concurrentUpdateFails_throwsOrderStatusInvalid() {
        // deliveryStatus 已经不是 pending（例如并发重复核销），条件更新 0 行
        Order order = pickupOrder("paid", "pending", STORE_A_ID);
        when(orderService.getOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(storeUserService.getByIdWithTenant(CLERK_USER_ID)).thenReturn(owner());
        when(orderService.update(any(LambdaUpdateWrapper.class))).thenReturn(false);

        assertThrows(BusinessException.class, () -> pickupVerifyService.verify("123456", CLERK_USER_ID));
        verify(verifyLogService, never()).save(any());
    }
}
