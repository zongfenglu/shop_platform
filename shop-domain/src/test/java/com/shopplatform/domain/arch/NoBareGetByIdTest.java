package com.shopplatform.domain.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;

/**
 * 越权防御静态检查（生产口径，无 allowEmptyShould）。见文档三 §2.4。
 * <p>
 * 本模块是业务 Service 真正落地的地方（goods/order/pay/... 各领域包），
 * 一旦有人写出 {@code xxxService.getById(id)} 这种绕过租户校验的调用，
 * CI 在这里会直接报红——这条规则不是摆设，Sprint 3 商品域 Service 落地后已经在真实拦截。
 * <p>
 * 按方法名匹配，不锁定具体接口签名——MyBatis-Plus 3.5.9 把 getById 的声明从 IService
 * 挪到了新引入的 IRepository 父接口且参数类型也变了，按精确签名匹配在这种库内部重构下会
 * 静默失效（零匹配但测试仍然"通过"，看起来在防护实际什么都没拦住）。这个坑已经真实踩过一次。
 */
class NoBareGetByIdTest {

    @Test
    void serviceLayerMustNotCallBareGetById() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.shopplatform.domain");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().callMethodWhere(target(name("getById")))
                .because("必须使用 TenantSafeService#getByIdWithTenant，禁止裸用 getById 绕过租户校验（文档三 §2.4）");
        // 不再 allowEmptyShould：Sprint 3 起 shop-domain 已有真实 Service 类，
        // 如果这条规则重新变成"零匹配"，本身就是一种异常信号（说明包扫描路径配错了），
        // 现在让它保持默认行为——零匹配也会让测试失败，倒逼排查而不是悄悄放行。

        rule.check(classes);
    }
}
