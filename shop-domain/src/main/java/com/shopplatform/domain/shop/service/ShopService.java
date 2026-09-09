package com.shopplatform.domain.shop.service;

import com.shopplatform.framework.mybatis.TenantSafeService;
import com.shopplatform.domain.shop.entity.Shop;

import java.util.List;

public interface ShopService extends TenantSafeService<Shop> {

    /**
     * 建店流程：创建商城主体 + 灌入种子数据（默认分类/运费模板/装修页/交易设置/会员等级/协议）+
     * 创建超级店主账号 + 写套餐快照。
     * <p>
     * 要求幂等可重试、整体耗时 &lt; 5 秒（文档二 §1.2）。幂等依据 code（二级域名前缀）唯一约束，
     * 重复调用同一个 code 不会产生重复商城，而是直接返回已存在的记录。
     */
    Shop createShop(CreateShopCommand command);

    /** 按二级域名前缀查找商城，登录场景专用——此时还不知道 shopId，只能靠 code 反查。 */
    Shop findByCode(String code);

    /**
     * 列出全部商城 id，供全租户循环类定时任务（如会员等级升级）按 shop 逐个 set/clear 租户上下文。
     * shop 表在 TenantLineInnerInterceptor 忽略表白名单里，无需 TenantContext.ignoreTenant 包裹。
     */
    List<Long> listAllShopIds();

    /** 平台超管重置该店超级店主密码。 */
    void resetOwnerPassword(Long shopId, String newPassword);

    /** 停用商城：商户后台不可登录，用户端按状态拦截。已停用则幂等。 */
    void disable(Long shopId);

    /**
     * 重新启用已停用商城。到期时间已过则回到 expired，否则回到 normal。
     * 已归档不能走这条接口。
     */
    void enable(Long shopId);

    /** 平台超管修改商城基本信息（名称/域名前缀/行业/联系人/手机/备注）。*/
    void updateInfo(Long shopId, UpdateShopCommand command);

    record CreateShopCommand(
            String name,
            String code,
            String industry,
            String contact,
            String mobile,
            String remark,
            Long packageTplId,
            Integer durationMonth
    ) {
    }

    /**
     * 平台超管可修改的字段：名称、域名前缀(code)、行业、联系人、手机、备注。
     * null 字段表示不修改（保留原值）。
     */
    record UpdateShopCommand(
            String name,
            String code,
            String industry,
            String contact,
            String mobile,
            String remark
    ) {
    }
}
