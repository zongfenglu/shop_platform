package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.ExchangeRecord;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface ExchangeRecordService extends TenantSafeService<ExchangeRecord> {

    /** 创建兑换单（仅校验，不扣库存不扣积分）。返回 status=unpaid 的记录。 */
    ExchangeRecord create(Long userId, Long pointsGoodsId);

    /** 支付兑换单：事务内先原子扣库存 → 再原子扣积分 → 发券/标记待发货。 */
    ExchangeRecord pay(Long userId, Long exchangeId);

    /** 按状态查询兑换记录（商户管理端发货队列）。 */
    List<ExchangeRecord> listByShop(String status);
}
