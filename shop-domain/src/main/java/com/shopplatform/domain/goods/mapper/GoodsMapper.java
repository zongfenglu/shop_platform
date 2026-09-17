package com.shopplatform.domain.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.goods.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /** SKU 库存扣减后同步扣减商品聚合库存，供商品详情直接展示。 */
    @Update("UPDATE goods SET stock_total = GREATEST(COALESCE(stock_total, 0) - #{quantity}, 0) "
            + "WHERE id = #{goodsId} AND is_delete = 0")
    int decreaseStockTotal(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);

    /** 关单或退款回补 SKU 时同步回补商品聚合库存。 */
    @Update("UPDATE goods SET stock_total = COALESCE(stock_total, 0) + #{quantity} "
            + "WHERE id = #{goodsId} AND is_delete = 0")
    int increaseStockTotal(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);

    /** 订单首次支付成功后按购买数量原子累加真实销量。 */
    @Update("UPDATE goods SET sales_actual = COALESCE(sales_actual, 0) + #{quantity} "
            + "WHERE id = #{goodsId} AND is_delete = 0")
    int increaseSalesActual(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);
}
