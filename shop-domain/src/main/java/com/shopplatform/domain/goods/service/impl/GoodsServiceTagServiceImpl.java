package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.goods.entity.GoodsServiceTag;
import com.shopplatform.domain.goods.mapper.GoodsServiceTagMapper;
import com.shopplatform.domain.goods.service.GoodsServiceTagService;
import org.springframework.stereotype.Service;

@Service
public class GoodsServiceTagServiceImpl extends ServiceImpl<GoodsServiceTagMapper, GoodsServiceTag> implements GoodsServiceTagService {
}
