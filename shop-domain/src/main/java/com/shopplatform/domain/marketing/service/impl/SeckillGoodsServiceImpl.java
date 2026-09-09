package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.mapper.SeckillGoodsMapper;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeckillGoodsServiceImpl extends ServiceImpl<SeckillGoodsMapper, SeckillGoods> implements SeckillGoodsService {

    @Override
    public List<SeckillGoods> listByActive(Long activeId) {
        return list(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActiveId, activeId)
                .orderByAsc(SeckillGoods::getSort)
                .orderByAsc(SeckillGoods::getId));
    }

    @Override
    public SeckillGoods findByActiveAndSku(Long activeId, Long skuId) {
        return getOne(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActiveId, activeId)
                .eq(SeckillGoods::getSkuId, skuId)
                .eq(SeckillGoods::getStatus, "on"), false);
    }

    @Override
    public boolean existsByActiveAndSku(Long activeId, Long skuId, Long excludeId) {
        return count(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActiveId, activeId)
                .eq(SeckillGoods::getSkuId, skuId)
                .ne(excludeId != null, SeckillGoods::getId, excludeId)) > 0;
    }

    @Override
    public void removeByActive(Long activeId) {
        remove(new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getActiveId, activeId));
    }

    @Override
    public boolean incrSold(Long id, int qty) {
        return baseMapper.incrSold(id, qty) > 0;
    }
}
