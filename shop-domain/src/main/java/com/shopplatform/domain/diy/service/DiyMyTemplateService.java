package com.shopplatform.domain.diy.service;

import com.shopplatform.domain.diy.entity.DiyMyTemplate;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface DiyMyTemplateService extends TenantSafeService<DiyMyTemplate> {

    /** 当前商户的私有模板列表，按更新时间倒序。 */
    List<DiyMyTemplate> listMine();

    /** 将草稿保存为一个新的私有模板，不做同名去重/覆盖。 */
    DiyMyTemplate save(String name, String pageDataJson);
}
