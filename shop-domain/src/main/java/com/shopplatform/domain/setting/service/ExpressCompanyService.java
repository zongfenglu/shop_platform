package com.shopplatform.domain.setting.service;

import com.shopplatform.domain.setting.entity.ExpressCompany;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface ExpressCompanyService extends TenantSafeService<ExpressCompany> {
    List<ExpressCompany> listAllWithDefaults();
    List<ExpressCompany> listEnabled();
    void seedDefaults();
}
