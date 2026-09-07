package com.shopplatform.domain.offlinestore.service;

import com.shopplatform.domain.order.entity.Order;

/** 自提核销：门店店员/店主输入核销码，核销一笔自提订单。 */
public interface PickupVerifyService {

    /**
     * 核销。校验：核销码存在且对应订单已支付、订单尚未核销（deliveryStatus=pending 的条件更新兜底防重复核销）、
     * 操作人若为"仅本店"数据权限的店员则订单的自提门店必须是其所属门店（否则视为越权，403）。
     */
    Order verify(String verifyCode, Long operatorUserId);
}
