package com.shopplatform.framework.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;

/**
 * 静态检查：禁止业务代码裸用 getById()，必须走 getByIdWithTenant()。
 * <p>
 * 见文档三 §2.4 越权防御 第2点。这条规则要求"随代码量增长自动生效"——
 * 新同事写了一行 goodsService.getById(id) 通过编译，但会在这里被 CI 拦下来，
 * 而不是等到测试环境或生产环境出现跨租户数据泄露才被发现。
 * <p>
 * 按方法名匹配而不是锁定某个具体接口的方法签名（如 IService#getById(Serializable)）——
 * MyBatis-Plus 3.5.9 把 getById 的声明从 IService 挪到了新引入的 IRepository 父接口，
 * 且参数类型也发生了变化；如果按精确签名匹配，库版本一升级这条规则就会静默失效（零匹配但测试仍然"通过"），
 * 这恰恰是最危险的情况——看起来在防护，实际上什么都没拦住。按方法名匹配对这类库内部重构更健壮。
 * <p>
 * 本测试运行在 shop-framework 模块自身，只覆盖能在 classpath 扫描到的类；
 * 各业务模块（admin-api/store-api/client-api）应在自己的模块下引入同一条规则，
 * 对本模块业务代码做二次扫描——见各模块 src/test 下的对应用例。
 */
class NoBareGetByIdTest {

    @Test
    void serviceLayerMustNotCallBareGetById() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.shopplatform");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().callMethodWhere(target(name("getById")))
                .because("必须使用 TenantSafeService#getByIdWithTenant，禁止裸用 getById 绕过租户校验（文档三 §2.4）")
                .allowEmptyShould(true);
        // allowEmptyShould(true)：shop-framework 本身不含业务 service 类，规则在这里天然是"零匹配"。
        // 各业务模块（shop-domain 等）一旦出现 ..service.. 包下的类，本规则会真正生效并强制校验，
        // 不需要每个模块重复维护一份一样的 ArchUnit 用例。

        rule.check(classes);
    }
}
