package com.shopplatform.domain.diy.service;

import com.shopplatform.domain.diy.entity.DiyTemplate;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface DiyTemplateService extends TenantSafeService<DiyTemplate> {

    /** 展示中的行业模板，按sort排序。 */
    List<DiyTemplate> listShown();
}
