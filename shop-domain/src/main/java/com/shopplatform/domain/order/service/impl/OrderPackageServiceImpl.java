package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.order.entity.OrderPackage;
import com.shopplatform.domain.order.mapper.OrderPackageMapper;
import com.shopplatform.domain.order.service.OrderPackageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderPackageServiceImpl extends ServiceImpl<OrderPackageMapper, OrderPackage>
        implements OrderPackageService {

    @Override
    public List<OrderPackage> listByOrderId(Long orderId) {
        return this.list(Wrappers.<OrderPackage>lambdaQuery().eq(OrderPackage::getOrderId, orderId));
    }
}
