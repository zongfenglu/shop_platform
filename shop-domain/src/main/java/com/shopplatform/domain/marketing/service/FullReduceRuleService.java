package com.shopplatform.domain.marketing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shopplatform.domain.marketing.entity.FullReduceRule;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface FullReduceRuleService extends TenantSafeService<FullReduceRule> {

    /** 当前生效的满减规则，按 sort 降序、id 降序 */
    List<FullReduceRule> listActive();
}
