package com.shopplatform.domain.shop.service;

/**
 * 租户到期检查。对应文档三 §6「租户到期检查 每天 00:05」。
 * 试用/正常且 expire_time 已过的商城转为 expired；停用/归档不动。
 */
public interface ShopExpireService {

    /** 返回本次新置为 expired 的商城数。 */
    int expireDue();
}
