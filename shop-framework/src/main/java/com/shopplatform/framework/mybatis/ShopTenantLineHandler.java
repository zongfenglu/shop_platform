package com.shopplatform.framework.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.shopplatform.framework.tenant.TenantContext;

/**
 * 租户行级过滤规则：告诉 MyBatis-Plus 的 TenantLineInnerInterceptor
 * 哪些表需要自动拼接 shop_id 条件、哪些表豁免。
 * <p>
 * 见文档三 §2.3 数据层自动隔离。忽略表清单只允许追加、不允许因为"图方便"随意扩大——
 * 每加一张表就意味着这张表的越权防线交给了业务代码手动兜底。
 */
public class ShopTenantLineHandler implements TenantLineHandler {

    /**
     * 平台级表白名单：这些表本身没有 shop_id 列，或属于跨租户共享的字典/平台数据。
     * 对应文档三 §3.1 平台层表 + 公共字典。
     */
    private static final Set<String> IGNORE_TABLES = Set.of(
            // 平台层：商城主体与订购
            "shop", "shop_package", "package_tpl", "shop_order", "shop_domain", "shop_quota_usage",
            "stat_shop_daily", "stat_goods_daily", "shop_invoice", "ops_backup",
            // 平台账号与设置
            "platform_user", "platform_role", "platform_role_menu", "platform_user_role", "platform_menu", "platform_setting",
            "mp_authorizer", "mp_release_task", "mp_component_ticket", "mp_code_template",
            "acme_account",
            // 公共字典
            "region", "express_company",
            // 装修：行业模板库（平台级，商户"一键套用"生成自己的 diy_page）
            "diy_template",
            // 日志（shop_id 可空，by_platform 场景不挂租户）
            "sys_log",
            // Flyway 自身表
            "flyway_schema_history"
    );

    @Override
    public Expression getTenantId() {
        Long shopId = TenantContext.getRequired();
        return new LongValue(shopId);
    }

    @Override
    public String getTenantIdColumn() {
        return "shop_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (TenantContext.isIgnoreTenant()) {
            return true;
        }
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }

    /** 供 ArchUnit / 集成测试引用，避免与拦截器配置产生第二份口径不一致的清单。 */
    public static List<String> ignoreTablesSnapshot() {
        return Arrays.asList(IGNORE_TABLES.toArray(new String[0]));
    }
}
