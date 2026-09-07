package com.shopplatform.framework.mybatis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shopplatform.common.exception.TenantAccessDeniedException;

/**
 * 租户安全的 Service 基础接口。
 * <p>
 * 虽然 {@link ShopTenantLineHandler} 已经在 SQL 层自动拼接 shop_id 条件，
 * 但文档三 §2.4 要求业务代码显式走 {@code getByIdWithTenant}，而不是裸用 {@code getById}：
 * <ul>
 *   <li>意图更明确——读代码的人一眼看出这是"按租户边界查询"，不用去翻拦截器配置才能确认安全性；</li>
 *   <li>可测试——ArchUnit 规则可以静态扫描出所有违规的裸用，见 {@code shop-framework} 测试模块；</li>
 *   <li>防御纵深——万一某个 Mapper 方法被标了 {@code @IgnoreTenant} 或拦截器配置被改错，
 *       这层显式校验仍然兜底。</li>
 * </ul>
 * 所有 Service 应继承本接口而不是直接用 MyBatis-Plus 的 {@link IService#getById}。
 */
public interface TenantSafeService<T> extends IService<T> {

    /**
     * 按主键查询，查不到（包括存在但不属于当前租户）统一抛 404 语义的业务异常，
     * 不返回 null 让调用方自己判断——避免"忘记判空"导致的隐性越权。
     */
    default T getByIdWithTenant(Long id) {
        T entity = this.getById(id);
        if (entity == null) {
            throw new TenantAccessDeniedException("id=" + id);
        }
        return entity;
    }
}
