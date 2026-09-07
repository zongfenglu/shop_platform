package com.shopplatform.domain.shop.service;

/** 租户到期前提醒。无短信通道时只写 sys_log，窗口为 7/3/1 天。 */
public interface ShopExpireRemindService {

    int remindDue();
}
