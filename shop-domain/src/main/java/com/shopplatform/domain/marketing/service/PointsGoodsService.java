package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.PointsGoods;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface PointsGoodsService extends TenantSafeService<PointsGoods> {

    /** 在架兑换项列表（消费者端，status=on，按 sort asc / id desc 排序） */
    List<PointsGoods> listOnSale();

    /** 全部兑换项（商户管理端，按 sort asc / id desc） */
    List<PointsGoods> listAll();
}
