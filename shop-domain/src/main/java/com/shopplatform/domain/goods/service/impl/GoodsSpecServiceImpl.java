package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.goods.entity.GoodsSpec;
import com.shopplatform.domain.goods.mapper.GoodsSpecMapper;
import com.shopplatform.domain.goods.service.GoodsSpecService;
import org.springframework.stereotype.Service;

@Service
public class GoodsSpecServiceImpl extends ServiceImpl<GoodsSpecMapper, GoodsSpec> implements GoodsSpecService {
}
