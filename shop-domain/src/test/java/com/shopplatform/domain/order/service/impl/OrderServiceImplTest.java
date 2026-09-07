package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.entity.OrderPackage;
import com.shopplatform.domain.order.mq.OrderCloseDelayProducer;
import com.shopplatform.domain.order.service.OrderAddressService;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderPackageService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pricing.OrderPriceResult;
import com.shopplatform.domain.pricing.PriceCalculator;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link OrderServiceImpl} 核心链路单测：下单减库存失败回滚、取消订单回补库存与幂等、
 * 支付状态流转幂等。见开发计划 Sprint 4/5 验收标准。
 * <p>
 * 不引入 Spring 上下文，用 {@link Mockito#spy} 包一层，直接对 {@code save}/{@code update}/
 * {@code getByIdWithTenant} 打桩——这些方法内部依赖真实 MyBatis-Plus 的 baseMapper/SqlSession，
 * 在没有真实 DB 的场景下不可执行，打桩后可以只验证"业务分支逻辑"本身，
 * 真正的落库行为已经有 Sprint1 的 {@code CrossTenantAccessDeniedIT} 之类的 Testcontainers 集成测试兜底。
 * <p>
 * {@code createOrder} 内部用 {@code TransactionSynchronizationManager.registerSynchronization}
 * 延迟发送关单消息，这一步要求"事务同步已激活"，否则抛 IllegalStateException——
 * 测试里用 {@code initSynchronization()} 模拟激活，而不需要拉起真实事务管理器。
 */
class OrderServiceImplTest {

    private PriceCalculator priceCalculator;
    private GoodsSkuService goodsSkuService;
    private OrderGoodsService orderGoodsService;
    private OrderAddressService orderAddressService;
    private OrderPackageService orderPackageService;
    private OrderCloseDelayProducer orderCloseDelayProducer;
    private UserCouponService userCouponService;
    private OfflineStoreService offlineStoreService;
    private OrderServiceImpl orderService;

    /**
     * MyBatis-Plus 的 {@code LambdaQueryWrapper}/{@code LambdaUpdateWrapper} 依赖启动期由
     * Spring Boot 自动配置扫描 Mapper 时建立的实体 lambda 缓存（{@code TableInfoHelper.initTableInfo}），
     * 本测试不拉起完整 Spring 上下文，需要手工触发一次初始化，否则 {@code Order::getPayStatus} 这类
     * 方法引用在运行时找不到缓存会抛 MybatisPlusException，而不是我们想验证的业务逻辑异常。
     */
    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Order.class);
    }

    @BeforeEach
    void setUp() {
        priceCalculator = mock(PriceCalculator.class);
        goodsSkuService = mock(GoodsSkuService.class);
        orderGoodsService = mock(OrderGoodsService.class);
        orderAddressService = mock(OrderAddressService.class);
        orderPackageService = mock(OrderPackageService.class);
        orderCloseDelayProducer = mock(OrderCloseDelayProducer.class);
        userCouponService = mock(UserCouponService.class);
        offlineStoreService = mock(OfflineStoreService.class);

        OrderServiceImpl impl = new OrderServiceImpl(priceCalculator, goodsSkuService, orderGoodsService,
                orderAddressService, orderPackageService, orderCloseDelayProducer, userCouponService,
                offlineStoreService, new ObjectMapper());
        orderService = spy(impl);

        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return true;
        }).when(orderService).save(any(Order.class));

        TenantContext.set(999L);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TenantContext.clear();
    }

    @Test
    void createOrder_deductsStockForEachItem_thenPersistsOrderAndOrderGoods() {
        stubPriceResult();
        when(goodsSkuService.deductStock(101L, 2)).thenReturn(true);

        Order order = orderService.createOrder(command());

        assertNotNull(order.getId());
        verify(goodsSkuService).deductStock(101L, 2);
        verify(orderGoodsService).saveBatch(anyList());
        verify(orderAddressService).save(any());
    }

    @Test
    void createOrder_stockInsufficient_throwsAndNeverPersistsOrder() {
        stubPriceResult();
        when(goodsSkuService.deductStock(101L, 2)).thenReturn(false);

        assertThrows(BusinessException.class, () -> orderService.createOrder(command()));

        verify(orderService, never()).save(any(Order.class));
        verify(orderGoodsService, never()).saveBatch(anyList());
    }

    @Test
    void createOrder_expressDeliveryWithoutAddress_throwsBeforeTouchingStock() {
        OrderService.CreateOrderCommand cmd = new OrderService.CreateOrderCommand(
                100L, List.of(), "express", null, null, null, null, "none", null, null, "mp", null, null);

        assertThrows(BusinessException.class, () -> orderService.createOrder(cmd));

        verifyNoInteractions(goodsSkuService);
    }

    @Test
    void createOrder_delayCloseMessage_onlySentAfterTransactionCommits() {
        stubPriceResult();
        when(goodsSkuService.deductStock(101L, 2)).thenReturn(true);

        orderService.createOrder(command());

        verify(orderCloseDelayProducer, never()).sendDelayClose(any(), any(), anyInt());

        TransactionSynchronizationManager.getSynchronizations().forEach(
                org.springframework.transaction.support.TransactionSynchronization::afterCommit);

        verify(orderCloseDelayProducer).sendDelayClose(eq(999L), eq(1L), anyInt());
    }

    @Test
    void createOrder_withCoupon_marksUsedAfterOrderPersisted() {
        stubPriceResult();
        when(goodsSkuService.deductStock(101L, 2)).thenReturn(true);
        when(userCouponService.tryUse(777L, 1L)).thenReturn(true);

        OrderService.CreateOrderCommand cmd = commandWithCoupon(777L);
        orderService.createOrder(cmd);

        verify(userCouponService).tryUse(777L, 1L);
    }

    @Test
    void createOrder_couponAlreadyUsed_throwsAndRollsBackOrder() {
        stubPriceResult();
        when(goodsSkuService.deductStock(101L, 2)).thenReturn(true);
        when(userCouponService.tryUse(777L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> orderService.createOrder(commandWithCoupon(777L)));

        // 券核销失败应回滚整笔订单：订单已 save 但事务会回滚，这里仅断言核销动作确实被调用且失败即抛
        verify(userCouponService).tryUse(777L, 1L);
    }

    @Test
    void cancel_orderWithCoupon_releasesCouponBackToUnused() {
        Order order = new Order();
        order.setId(5L);
        order.setPayStatus("unpaid");
        order.setCouponId(777L);
        doReturn(order).when(orderService).getByIdWithTenant(5L);
        doReturn(true).when(orderService).update(any());
        when(orderGoodsService.listByOrderId(5L)).thenReturn(List.of());

        boolean cancelled = orderService.cancel(5L, "用户取消");

        assertTrue(cancelled);
        verify(userCouponService).release(777L, 5L);
    }

    @Test
    void cancel_unpaidOrder_restoresStockForEveryOrderGoodsLine() {
        Order order = new Order();
        order.setId(5L);
        order.setPayStatus("unpaid");
        doReturn(order).when(orderService).getByIdWithTenant(5L);
        doReturn(true).when(orderService).update(any());

        OrderGoods og = new OrderGoods();
        og.setSkuId(101L);
        og.setTotalNum(2);
        when(orderGoodsService.listByOrderId(5L)).thenReturn(List.of(og));

        boolean cancelled = orderService.cancel(5L, "用户取消");

        assertTrue(cancelled);
        verify(goodsSkuService).restoreStock(101L, 2);
    }

    /**
     * 支付回调先一步到达导致订单已是 paid 状态时，取消/关单动作必须作废且不回补库存，
     * 否则库存已经属于一笔已支付的订单，回补会导致超卖——见 OrderServiceImpl.cancel() 注释。
     */
    @Test
    void cancel_orderAlreadyPaid_doesNotRestoreStock_andReturnsFalse() {
        Order order = new Order();
        order.setId(5L);
        order.setPayStatus("paid");
        doReturn(order).when(orderService).getByIdWithTenant(5L);
        doReturn(false).when(orderService).update(any());

        boolean cancelled = orderService.cancel(5L, "用户取消");

        assertFalse(cancelled);
        verifyNoInteractions(goodsSkuService);
        verify(orderGoodsService, never()).listByOrderId(any());
    }

    @Test
    void cancel_crossTenantOrderId_bubbleUpTenantAccessDeniedBeforeAnyMutation() {
        doThrow(new com.shopplatform.common.exception.TenantAccessDeniedException("id=5"))
                .when(orderService).getByIdWithTenant(5L);

        assertThrows(com.shopplatform.common.exception.TenantAccessDeniedException.class,
                () -> orderService.cancel(5L, "越权尝试"));

        verify(orderService, never()).update(any());
        verifyNoInteractions(goodsSkuService);
    }

    @Test
    void markPaid_conditionalUpdate_delegatesToOptimisticStatusTransition() {
        doReturn(true).when(orderService).update(any());

        boolean result = orderService.markPaid("ORDER123", "wx_txn_1", "wechat");

        assertTrue(result);
        verify(orderService).update(any());
    }

    @Test
    void markPaid_orderAlreadyPaid_returnsFalse_callerShouldTreatAsAlreadyProcessed() {
        doReturn(false).when(orderService).update(any());

        boolean result = orderService.markPaid("ORDER123", "wx_txn_1", "wechat");

        assertFalse(result);
    }

    @Test
    void ship_partialThenRemaining_persistsDistinctPackages() {
        stubPaidPendingOrder(8L);
        when(orderGoodsService.listByOrderId(8L)).thenReturn(List.of(line(11L), line(12L)));
        when(orderPackageService.listByOrderId(8L)).thenReturn(List.of());
        doReturn(true).when(orderService).update(any());

        orderService.ship(8L, new OrderService.ShipCommand("顺丰", "SF1", List.of(11L)));

        org.mockito.ArgumentCaptor<OrderPackage> first = org.mockito.ArgumentCaptor.forClass(OrderPackage.class);
        verify(orderPackageService).save(first.capture());
        assertEquals("[11]", first.getValue().getOrderGoodsIds());

        OrderPackage existing = new OrderPackage();
        existing.setOrderGoodsIds("[11]");
        when(orderPackageService.listByOrderId(8L)).thenReturn(List.of(existing));
        Mockito.reset(orderPackageService);
        when(orderPackageService.listByOrderId(8L)).thenReturn(List.of(existing));

        orderService.ship(8L, new OrderService.ShipCommand("中通", "ZT1", List.of()));

        org.mockito.ArgumentCaptor<OrderPackage> second = org.mockito.ArgumentCaptor.forClass(OrderPackage.class);
        verify(orderPackageService).save(second.capture());
        assertEquals("[12]", second.getValue().getOrderGoodsIds());
        assertEquals("中通", second.getValue().getExpressCompany());
    }

    @Test
    void ship_alreadyShippedLine_throws() {
        stubPaidPendingOrder(8L);
        when(orderGoodsService.listByOrderId(8L)).thenReturn(List.of(line(11L), line(12L)));
        OrderPackage existing = new OrderPackage();
        existing.setOrderGoodsIds("[11]");
        when(orderPackageService.listByOrderId(8L)).thenReturn(List.of(existing));

        BusinessException e = assertThrows(BusinessException.class,
                () -> orderService.ship(8L, new OrderService.ShipCommand("顺丰", "SF2", List.of(11L))));
        assertTrue(e.getMessage().contains("已发货"));
        verify(orderPackageService, never()).save(any());
    }

    @Test
    void ship_legacyEmptyPackage_treatsWholeOrderAsShipped() {
        stubPaidPendingOrder(8L);
        when(orderGoodsService.listByOrderId(8L)).thenReturn(List.of(line(11L), line(12L)));
        OrderPackage existing = new OrderPackage();
        existing.setOrderGoodsIds("[]");
        when(orderPackageService.listByOrderId(8L)).thenReturn(List.of(existing));

        BusinessException e = assertThrows(BusinessException.class,
                () -> orderService.ship(8L, new OrderService.ShipCommand("顺丰", "SF3", List.of())));
        assertTrue(e.getMessage().contains("全部发出"));
    }

    @Test
    void ship_goodsNotOnOrder_throws() {
        stubPaidPendingOrder(8L);
        when(orderGoodsService.listByOrderId(8L)).thenReturn(List.of(line(11L)));
        when(orderPackageService.listByOrderId(8L)).thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> orderService.ship(8L, new OrderService.ShipCommand("顺丰", "SF4", List.of(99L))));
        verify(orderPackageService, never()).save(any());
    }

    private void stubPaidPendingOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setPayStatus("paid");
        order.setDeliveryStatus("pending");
        order.setDeliveryType("express");
        doReturn(order).when(orderService).getByIdWithTenant(id);
    }

    private static OrderGoods line(Long id) {
        OrderGoods og = new OrderGoods();
        og.setId(id);
        return og;
    }

    private void stubPriceResult() {
        OrderPriceResult.ItemResult item = new OrderPriceResult.ItemResult(
                10L, 101L, "测试商品", "默认", "img.jpg",
                new BigDecimal("50.00"), new BigDecimal("60.00"), 2,
                new BigDecimal("100.00"), Map.of());
        OrderPriceResult result = new OrderPriceResult(
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("100.00"), List.of(item));
        when(priceCalculator.calculate(any())).thenReturn(result);
    }

    private OrderService.CreateOrderCommand command() {
        return commandWithCoupon(null);
    }

    private OrderService.CreateOrderCommand commandWithCoupon(Long couponId) {
        OrderService.AddressInfo address = new OrderService.AddressInfo(
                "张三", "13800000000", "广东", "深圳", "南山区", "科技园");
        return new OrderService.CreateOrderCommand(
                100L,
                List.of(new PriceContext.PriceItem(10L, 101L, "测试商品", "默认", "img.jpg",
                        new BigDecimal("50.00"), new BigDecimal("60.00"), 2, null)),
                "express", null, null, couponId, null, "none", null, null, "mp", null, address);
    }
}
