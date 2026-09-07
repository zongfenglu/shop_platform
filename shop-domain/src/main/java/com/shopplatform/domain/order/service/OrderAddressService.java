package com.shopplatform.domain.order.service;

import com.shopplatform.domain.order.entity.OrderAddress;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface OrderAddressService extends TenantSafeService<OrderAddress> {

    OrderAddress findByOrderId(Long orderId);
}
