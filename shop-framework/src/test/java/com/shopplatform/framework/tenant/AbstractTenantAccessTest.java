package com.shopplatform.framework.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 越权测试脚手架基类。见文档一 §7 非功能性要求："自动化越权测试进 CI"，
 * 以及文档三 §2.4："CI 中跑租户 A 的 token 访问租户 B 的资源 ID，
 * 覆盖所有 /store/** 接口，任一返回非 403/404 即构建失败"。
 * <p>
 * 各业务模块（shop-store-api 等）应为每一个带路径参数的接口写一条子类测试，
 * 形如：
 * <pre>
 *   class GoodsControllerTenantAccessTest extends AbstractTenantAccessTest {
 *       @Test
 *       void getGoodsDetail_crossTenant_shouldBeDenied() {
 *           assertCrossTenantAccessDenied(() -> goodsService.getByIdWithTenant(otherShopGoodsId));
 *       }
 *   }
 * </pre>
 * 断言口径统一为"必须抛出 TenantAccessDeniedException"，
 * 而不是自己在每个测试里各写一套判断逻辑——保持这条防线的收口位置唯一。
 */
public abstract class AbstractTenantAccessTest {

    protected void assertCrossTenantAccessDenied(Executable action) {
        assertThrows(com.shopplatform.common.exception.TenantAccessDeniedException.class, action,
                "跨租户访问必须被拒绝（403/404 语义），本条测试是 CI 一票否决项，禁止跳过或放宽断言");
    }

    /** 本类本身不含可运行断言，仅作为基类；避免测试报告里出现"空测试类"的告警。 */
    @Test
    void scaffoldClassIsAbstractBase() {
        // 无操作：具体断言由子类的每个接口用例补充。
    }
}
