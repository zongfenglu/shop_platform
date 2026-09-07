package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.BargainActive;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface BargainActiveService extends TenantSafeService<BargainActive> {

    /** 列出当前租户上架的砍价活动 */
    List<BargainActive> listOnSale();

    /** 列出当前租户全部砍价活动（含下架，商户后台用） */
    List<BargainActive> listAll();
}
