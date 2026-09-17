package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.mapper.GoodsMapper;
import com.shopplatform.domain.goods.mapper.GoodsSkuMapper;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoodsSkuServiceImpl extends ServiceImpl<GoodsSkuMapper, GoodsSku> implements GoodsSkuService {

    private final GoodsMapper goodsMapper;

    public GoodsSkuServiceImpl(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    @Override
    public List<GoodsSku> listByGoodsId(Long goodsId) {
        return this.list(Wrappers.<GoodsSku>lambdaQuery().eq(GoodsSku::getGoodsId, goodsId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long skuId, int quantity) {
        // UPDATE goods_sku SET stock = stock - ? WHERE id = ? AND stock >= ?
        // shop_id 条件由 TenantLineInnerInterceptor 自动拼接，此处不需要手写；
        // BlockAttackInnerInterceptor 要求 update 必须带 where 条件，这里的 eq(id) 满足该要求。
        // 见文档三 §5 库存与并发："影响行数0即库存不足"。
        UpdateWrapper<GoodsSku> wrapper = new UpdateWrapper<>();
        wrapper.setSql("stock = stock - " + quantity)
                .eq("id", skuId)
                .ge("stock", quantity);
        boolean updated = this.update(wrapper);
        if (updated) {
            GoodsSku sku = this.getByIdWithTenant(skuId);
            goodsMapper.decreaseStockTotal(sku.getGoodsId(), quantity);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(Long skuId, int quantity) {
        UpdateWrapper<GoodsSku> wrapper = new UpdateWrapper<>();
        wrapper.setSql("stock = stock + " + quantity).eq("id", skuId);
        boolean updated = this.update(wrapper);
        if (updated) {
            GoodsSku sku = this.getByIdWithTenant(skuId);
            goodsMapper.increaseStockTotal(sku.getGoodsId(), quantity);
        }
    }
}
