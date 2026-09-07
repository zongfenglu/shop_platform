package com.shopplatform.domain.diy.service;

import com.shopplatform.domain.diy.entity.DiyTabbar;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface DiyTabbarService extends TenantSafeService<DiyTabbar> {

    /** 当前商城的底部导航，若不存在则创建默认4项（首页/分类/购物车/我的）后返回。 */
    DiyTabbar getOrCreateDefault();

    /** 只读：没有配置时返回 null，不自动建默认行。 */
    DiyTabbar peek();

    /** 校验items数组长度在2~5之间后保存，按shop单行upsert。 */
    DiyTabbar save(String itemsJson, String styleJson);
}
