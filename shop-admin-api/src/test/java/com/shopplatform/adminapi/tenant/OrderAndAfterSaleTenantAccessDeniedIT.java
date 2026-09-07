package com.shopplatform.adminapi.tenant;

import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 越权防御端到端验证：订单与售后单是全系统资金相关性最高的两类资源，M1 质量门禁要求
 * "全量越权测试覆盖 /store/**、/api/** 所有接口"（见开发计划 Sprint 6），本测试补齐
 * {@link CrossTenantAccessDeniedIT} 未覆盖的 Order/AfterSale 两类核心资源在
 * {@code getByIdWithTenant} 层面的越权拦截，与该类共用同一断言口径。
 */
@SpringBootTest
@Testcontainers
class OrderAndAfterSaleTenantAccessDeniedIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("shop_platform")
            .withUsername("root")
            .withPassword("root");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                mysql.getJdbcUrl() + "?allowPublicKeyRetrieval=true&useSSL=false");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private AfterSaleService afterSaleService;

    private Long shopAId;
    private Long shopBId;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void getOrderByIdUnderTenantA_withTenantBOrderId_mustReturnNull() {
        Long orderIdOfB = seedOrderForShopB();

        TenantContext.set(shopAId);
        boolean deniedAsExpected;
        try {
            orderService.getByIdWithTenant(orderIdOfB);
            deniedAsExpected = false;
        } catch (com.shopplatform.common.exception.TenantAccessDeniedException e) {
            deniedAsExpected = true;
        } finally {
            TenantContext.clear();
        }

        assertTrue(deniedAsExpected, "租户A的上下文下按主键查询租户B的订单，必须抛出越权异常而不是返回数据");
    }

    @Test
    void listOrdersUnderTenantA_mustNotIncludeTenantBOrders() {
        seedOrderForShopB();
        Long orderIdOfA = seedOrderForShopA();

        TenantContext.set(shopAId);
        Order visible = orderService.getByIdWithTenant(orderIdOfA);
        var allVisibleToA = orderService.list();
        TenantContext.clear();

        assertTrue(visible != null && visible.getShopId().equals(shopAId));
        assertTrue(allVisibleToA.stream().allMatch(o -> o.getShopId().equals(shopAId)),
                "租户A的list()查询结果里不应混入任何租户B的订单");
    }

    @Test
    void getAfterSaleByIdUnderTenantA_withTenantBAfterSaleId_mustReturnNull() {
        Long afterSaleIdOfB = seedAfterSaleForShopB();

        TenantContext.set(shopAId);
        boolean deniedAsExpected;
        try {
            afterSaleService.getByIdWithTenant(afterSaleIdOfB);
            deniedAsExpected = false;
        } catch (com.shopplatform.common.exception.TenantAccessDeniedException e) {
            deniedAsExpected = true;
        } finally {
            TenantContext.clear();
        }

        assertTrue(deniedAsExpected, "租户A的上下文下按主键查询租户B的售后单，必须抛出越权异常而不是返回数据");
    }

    private void initShopIds() {
        shopAId = System.nanoTime();
        shopBId = shopAId + 1;
    }

    private Long seedOrderForShopA() {
        if (shopAId == null) {
            initShopIds();
        }
        return seedOrder(shopAId);
    }

    private Long seedOrderForShopB() {
        if (shopBId == null) {
            initShopIds();
        }
        return seedOrder(shopBId);
    }

    private Long seedOrder(Long shopId) {
        TenantContext.set(shopId);
        Order order = new Order();
        order.setOrderNo("TEST" + System.nanoTime());
        order.setUserId(1L);
        order.setSellerId(0L);
        order.setTotalPrice(new BigDecimal("100.00"));
        order.setDiscountPrice(BigDecimal.ZERO);
        order.setCouponPrice(BigDecimal.ZERO);
        order.setPointsPrice(BigDecimal.ZERO);
        order.setExpressPrice(BigDecimal.ZERO);
        order.setPayPrice(new BigDecimal("100.00"));
        order.setPayStatus("unpaid");
        order.setDeliveryType("express");
        order.setDeliveryStatus("pending");
        order.setReceiptStatus("pending");
        order.setOrderStatus("normal");
        order.setOrderSource("mp");
        order.setActivityType("none");
        orderService.save(order);
        TenantContext.clear();
        assertTrue(order.getId() != null, "种子订单必须成功插入才能继续断言");
        return order.getId();
    }

    private Long seedAfterSaleForShopB() {
        if (shopBId == null) {
            initShopIds();
        }
        TenantContext.set(shopBId);
        AfterSale afterSale = new AfterSale();
        afterSale.setOrderId(1L);
        afterSale.setOrderGoodsId(1L);
        afterSale.setUserId(1L);
        afterSale.setType("refund_only");
        afterSale.setApplyReason("测试原因");
        afterSale.setRefundNum(1);
        afterSale.setRefundAmount(new BigDecimal("10.00"));
        afterSale.setStatus("applying");
        afterSale.setRefundNo("RFTEST" + System.nanoTime());
        afterSaleService.save(afterSale);
        TenantContext.clear();
        assertTrue(afterSale.getId() != null, "种子售后单必须成功插入才能继续断言");
        return afterSale.getId();
    }
}
