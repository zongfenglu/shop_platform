package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.goods.entity.GoodsSpecValue;
import com.shopplatform.domain.goods.mapper.GoodsSpecValueMapper;
import com.shopplatform.domain.goods.service.GoodsSpecValueService;
import org.springframework.stereotype.Service;

@Service
public class GoodsSpecValueServiceImpl extends ServiceImpl<GoodsSpecValueMapper, GoodsSpecValue> implements GoodsSpecValueService {
}
