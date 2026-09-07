package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.order.entity.OrderAddress;
import com.shopplatform.domain.order.mapper.OrderAddressMapper;
import com.shopplatform.domain.order.service.OrderAddressService;
import org.springframework.stereotype.Service;

@Service
public class OrderAddressServiceImpl extends ServiceImpl<OrderAddressMapper, OrderAddress> implements OrderAddressService {

    @Override
    public OrderAddress findByOrderId(Long orderId) {
        return this.getOne(Wrappers.<OrderAddress>lambdaQuery().eq(OrderAddress::getOrderId, orderId));
    }
}
