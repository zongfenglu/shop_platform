package com.shopplatform.domain.order.service;

import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface OrderGoodsService extends TenantSafeService<OrderGoods> {

    List<OrderGoods> listByOrderId(Long orderId);
}
