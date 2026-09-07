package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.SeckillTime;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface SeckillTimeService extends TenantSafeService<SeckillTime> {

    /** 当前商户全部场次，按 sort 升序、id 升序 */
    List<SeckillTime> listAll();
}
