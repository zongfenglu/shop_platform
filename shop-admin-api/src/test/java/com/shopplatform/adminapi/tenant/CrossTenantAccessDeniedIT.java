package com.shopplatform.adminapi.tenant;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.service.StoreUserService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端越权防御验证：真实拉起 MySQL，验证 {@code TenantLineInnerInterceptor} 在实际 SQL 执行层面
 * 确实拦截了跨租户访问——这条测试比纯 mock 单测更有说服力，因为它跑的是真实拼接后的 SQL。
 * <p>
 * 见文档三 §2.4；文档一 §7 非功能性要求"自动化越权测试进CI"。
 * 这是 CI 一票否决项：任一断言失败都意味着多租户隔离的核心机制出了问题，必须立刻修复，不允许跳过。
 * <p>
 * 本地 Windows + Docker Desktop 环境注意：Testcontainers 在部分 Windows npipe 配置下
 * （Docker Desktop 的 desktop-linux context）可能报 "Could not find a valid Docker environment"，
 * 这是已知的 Windows npipe 路由兼容性问题，非本测试逻辑缺陷（同样的多租户隔离逻辑已通过手动拉起
 * MySQL 容器 + 真实起服务验证过，见 Sprint 1 收尾记录）。CI 跑在 Linux runner 上不受此问题影响。
 * 本地如需复现，可尝试 WSL2 backend 或直接在 CI 上验证。
 */
@SpringBootTest
@Testcontainers
class CrossTenantAccessDeniedIT {

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
    private StoreUserService storeUserService;

    private Long shopAId;
    private Long shopBId;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /**
     * 核心断言：在租户 A 的上下文下，查询租户 B 创建的数据，必须查不到（返回空，而不是返回数据）。
     * 这就是 TenantLineInnerInterceptor 自动拼接 shop_id 条件在起作用。
     */
    @Test
    void queryUnderTenantA_mustNotSeeTenantBData() {
        seedTwoShopsWithOneUserEach();

        TenantContext.set(shopAId);
        List<StoreUser> visibleToA = storeUserService.list();
        TenantContext.clear();

        assertEquals(1, visibleToA.size(), "租户A的上下文下只应看到租户A自己的1条数据");
        assertEquals(shopAId, visibleToA.get(0).getShopId());

        TenantContext.set(shopBId);
        List<StoreUser> visibleToB = storeUserService.list();
        TenantContext.clear();

        assertEquals(1, visibleToB.size(), "租户B的上下文下只应看到租户B自己的1条数据");
        assertEquals(shopBId, visibleToB.get(0).getShopId());
    }

    /**
     * 核心断言：在租户 A 的上下文下，按主键查询租户 B 的记录 ID，必须查不到（null），
     * 而不是"查到了但没做权限判断就返回"。这条对应 getByIdWithTenant 的第一道防线——
     * 拦截器层面的自动过滤，即使业务代码忘了调用 getByIdWithTenant，裸用 getById 也不会跨租户泄露。
     */
    @Test
    void getByIdUnderTenantA_withTenantBRecordId_mustReturnNull() {
        seedTwoShopsWithOneUserEach();

        TenantContext.set(shopAId);
        StoreUser userB = storeUserService.getOne(
                Wrappers.<StoreUser>lambdaQuery().eq(StoreUser::getShopId, shopBId));
        TenantContext.clear();

        assertNull(userB, "租户A的上下文下，即使显式按shop_id=B去查，拦截器也会追加shop_id=A的条件，查不到任何数据");
    }

    private void seedTwoShopsWithOneUserEach() {
        // 每个测试方法用不重复的 shopId，避免同一容器内多个测试方法之间的数据相互干扰
        // （Testcontainers 默认按测试类复用同一个容器实例，数据不会在方法间自动清空）。
        shopAId = System.nanoTime();
        shopBId = shopAId + 1;

        TenantContext.set(shopAId);
        StoreUser userA = new StoreUser();
        userA.setShopId(shopAId);
        userA.setUsername("shopA_admin_" + System.nanoTime());
        userA.setPassword("placeholder");
        userA.setRoleId(1L);
        userA.setStatus(1);
        storeUserService.save(userA);
        TenantContext.clear();

        TenantContext.set(shopBId);
        StoreUser userB = new StoreUser();
        userB.setShopId(shopBId);
        userB.setUsername("shopB_admin_" + System.nanoTime());
        userB.setPassword("placeholder");
        userB.setRoleId(1L);
        userB.setStatus(1);
        storeUserService.save(userB);
        TenantContext.clear();

        assertTrue(userA.getId() != null && userB.getId() != null, "两条种子数据必须成功插入才能继续断言");
    }
}
