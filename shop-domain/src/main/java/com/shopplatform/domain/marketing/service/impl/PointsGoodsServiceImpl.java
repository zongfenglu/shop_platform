package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.PointsGoods;
import com.shopplatform.domain.marketing.mapper.PointsGoodsMapper;
import com.shopplatform.domain.marketing.service.PointsGoodsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointsGoodsServiceImpl extends ServiceImpl<PointsGoodsMapper, PointsGoods> implements PointsGoodsService {

    @Override
    public List<PointsGoods> listOnSale() {
        return list(new LambdaQueryWrapper<PointsGoods>()
                .eq(PointsGoods::getStatus, "on")
                .orderByAsc(PointsGoods::getSort)
                .orderByDesc(PointsGoods::getId));
    }

    @Override
    public List<PointsGoods> listAll() {
        return list(new LambdaQueryWrapper<PointsGoods>()
                .orderByAsc(PointsGoods::getSort)
                .orderByDesc(PointsGoods::getId));
    }
}
