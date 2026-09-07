package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.mapper.OrderGoodsMapper;
import com.shopplatform.domain.order.service.OrderGoodsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderGoodsServiceImpl extends ServiceImpl<OrderGoodsMapper, OrderGoods> implements OrderGoodsService {

    @Override
    public List<OrderGoods> listByOrderId(Long orderId) {
        return this.list(Wrappers.<OrderGoods>lambdaQuery().eq(OrderGoods::getOrderId, orderId));
    }
}
