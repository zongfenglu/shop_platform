package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.mapper.ShopOrderMapper;
import com.shopplatform.domain.shop.service.ShopOrderService;
import org.springframework.stereotype.Service;

@Service
public class ShopOrderServiceImpl extends ServiceImpl<ShopOrderMapper, ShopOrder> implements ShopOrderService {
}
