package com.shopplatform.domain.member.service;

import com.shopplatform.domain.member.entity.RechargePlan;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface RechargePlanService extends TenantSafeService<RechargePlan> {

    /** 列出上架的充值方案（is_show=1），按 sort 升序，供消费者端展示。 */
    List<RechargePlan> listShown();
}
