package com.shopplatform.framework.tenant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Mapper 方法上，声明该 SQL 不自动拼接 shop_id 过滤条件。
 * 仅限平台侧统计等显式声明的跨租户查询使用（见文档三 §2.3）。
 * <p>
 * 使用该注解前必须在 Service 层已通过 {@link TenantContext#ignoreTenant} 显式声明意图，
 * 不允许业务代码"偶然"绕过租户隔离。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IgnoreTenant {
}
