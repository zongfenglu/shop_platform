package com.shopplatform.domain.pay.service;

import com.shopplatform.domain.pay.entity.PayNotifyLog;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface PayNotifyLogService extends TenantSafeService<PayNotifyLog> {

    /**
     * 幂等登记：以 (shop_id, transaction_id) 唯一约束为准，先插入后处理业务——
     * 插入成功才继续走订单状态流转，插入因唯一键冲突失败则说明已经处理过，直接返回 false。
     * 用"插入是否成功"判断而不是"先查后插"，是为了让并发重复投递也能被数据库唯一索引兜底，
     * 不依赖应用层的 check-then-act（那样在两个请求线程之间存在天然的竞态窗口）。
     */
    boolean tryMarkProcessed(String channel, String transactionId, String outTradeNo);
}
