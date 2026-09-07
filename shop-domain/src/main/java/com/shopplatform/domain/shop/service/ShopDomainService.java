package com.shopplatform.domain.shop.service;

import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface ShopDomainService extends TenantSafeService<ShopDomain> {

    /**
     * 按域名反查已生效绑定，供消费者端 Host 识别使用（见 ClientTenantFilter）。
     * 未审核通过的自定义域名不参与识别。
     */
    ShopDomain findByDomain(String domain);

    /** 不限审核状态，用于申请时的唯一性校验。 */
    ShopDomain findAnyByDomain(String domain);

    java.util.List<ShopDomain> listByShopId(Long shopId);
}
