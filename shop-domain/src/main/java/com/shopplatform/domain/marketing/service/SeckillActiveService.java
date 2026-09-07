package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface SeckillActiveService extends TenantSafeService<SeckillActive> {

    /** 今日有效（含今天、status=on）的活动，按 id 降序 */
    List<SeckillActive> listOnSale();

    /** 当前商户全部活动，按 id 降序 */
    List<SeckillActive> listAll();
}
