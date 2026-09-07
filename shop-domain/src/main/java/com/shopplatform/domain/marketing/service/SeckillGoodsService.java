package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface SeckillGoodsService extends TenantSafeService<SeckillGoods> {

    /** 某活动下全部商品，按 sort 升序、id 升序 */
    List<SeckillGoods> listByActive(Long activeId);

    /** 按 (activeId, skuId) 查命中且上架的商品；未命中返回 null */
    SeckillGoods findByActiveAndSku(Long activeId, Long skuId);

    /** 秒杀下单成功后累加已售（带 seckill_num 上限校验）。返回 false 表示已售罄/状态异常。 */
    boolean incrSold(Long id, int qty);
}
