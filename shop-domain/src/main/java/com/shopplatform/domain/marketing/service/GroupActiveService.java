package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface GroupActiveService extends TenantSafeService<GroupActive> {

    /** 列出当前租户上架的拼团活动 */
    List<GroupActive> listOnSale();

    /** 列出当前租户全部拼团活动（含下架，商户后台用） */
    List<GroupActive> listAll();

    /** 解析 group_price JSON 为 {skuId -> 拼团价} */
    Map<Long, BigDecimal> parseGroupPrice(GroupActive active);
}
