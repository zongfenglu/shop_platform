package com.shopplatform.domain.shop.service;

/**
 * 配额用量日快照。对应文档三 §6「配额用量统计 每天 01:00 写 shop_quota_usage」。
 * 全租户循环在本服务内完成，单租户异常不中断整批。
 */
public interface ShopQuotaSnapshotService {

    /**
     * 为全部商城写入（或覆盖）指定统计日的用量快照。返回成功写入的商城数。
     */
    int snapshotAll();
}
