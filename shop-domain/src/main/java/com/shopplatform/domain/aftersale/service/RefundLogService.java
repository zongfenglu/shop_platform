package com.shopplatform.domain.aftersale.service;

import com.shopplatform.domain.aftersale.entity.RefundLog;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface RefundLogService extends TenantSafeService<RefundLog> {

    /**
     * 独立事务写入：{@link com.shopplatform.domain.aftersale.service.impl.AfterSaleServiceImpl#executeRefund}
     * 整体是一个大事务，失败时会连同"售后单状态从 refunding 回退到 approved"一起回滚——
     * 但失败流水本身恰恰是最需要留痕的场景（排查退款为什么没成功），所以流水写入必须开新事务提交，
     * 不能跟着外层事务一起被回滚掉。
     */
    void saveIndependently(RefundLog log);
}
