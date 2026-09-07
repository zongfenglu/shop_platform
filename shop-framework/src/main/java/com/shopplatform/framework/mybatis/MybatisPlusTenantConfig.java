package com.shopplatform.framework.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 多租户拦截器配置。
 * <p>
 * {@link TenantLineInnerInterceptor} 会对 SELECT/UPDATE/DELETE 自动拼接 shop_id 条件，
 * 对 INSERT 自动填充 shop_id 列——这是"防漏"的第一道防线。
 * <p>
 * {@link BlockAttackInnerInterceptor} 拦截不带 where 条件的全表 update/delete，
 * 防止业务代码误写导致跨租户批量污染数据（这是"防错"的第二道防线，
 * 见文档三 §2.4 越权防御 第1点"写操作二次校验"）。
 * <p>
 * 拦截器顺序不能随意调整：{@link TenantLineInnerInterceptor} 必须排在
 * {@link PaginationInnerInterceptor} 之前——分页插件会先把原始 SQL 包装成 COUNT 查询与分页查询两条语句，
 * 如果租户过滤条件在分页包装之后才拼接，会导致 COUNT 语句没有被正确拼上 shop_id 条件，
 * 分页总数统计出现跨租户计数错误。
 */
@Configuration
public class MybatisPlusTenantConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new ShopTenantLineHandler()));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
