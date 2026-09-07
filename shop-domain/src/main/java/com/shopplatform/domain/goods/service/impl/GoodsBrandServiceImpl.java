package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.goods.entity.GoodsBrand;
import com.shopplatform.domain.goods.mapper.GoodsBrandMapper;
import com.shopplatform.domain.goods.service.GoodsBrandService;
import org.springframework.stereotype.Service;

@Service
public class GoodsBrandServiceImpl extends ServiceImpl<GoodsBrandMapper, GoodsBrand> implements GoodsBrandService {
}
