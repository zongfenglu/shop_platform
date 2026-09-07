package com.shopplatform.domain.goods.service;

import com.shopplatform.framework.mybatis.TenantSafeService;
import com.shopplatform.domain.goods.entity.GoodsSku;

import java.util.List;

public interface GoodsSkuService extends TenantSafeService<GoodsSku> {

    List<GoodsSku> listByGoodsId(Long goodsId);

    /**
     * 乐观锁扣减库存：{@code UPDATE goods_sku SET stock=stock-? WHERE id=? AND shop_id=? AND stock>=?}，
     * 影响行数为0即库存不足。见文档三 §5 库存与并发："普通下单走DB乐观扣减"。
     *
     * @return true 表示扣减成功；false 表示库存不足（不抛异常，由调用方决定如何处理，如批量下单场景需要整单回滚）
     */
    boolean deductStock(Long skuId, int quantity);

    /** 售后退款/超时关单回补库存。 */
    void restoreStock(Long skuId, int quantity);
}
