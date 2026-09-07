package com.shopplatform.domain.order.service;

import com.shopplatform.domain.order.entity.OrderPackage;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface OrderPackageService extends TenantSafeService<OrderPackage> {

    List<OrderPackage> listByOrderId(Long orderId);
}
