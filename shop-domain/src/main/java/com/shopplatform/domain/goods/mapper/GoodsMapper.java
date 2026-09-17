package com.shopplatform.domain.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.goods.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /** 订单首次支付成功后按购买数量原子累加真实销量。 */
    @Update("UPDATE goods SET sales_actual = COALESCE(sales_actual, 0) + #{quantity} "
            + "WHERE id = #{goodsId} AND is_delete = 0")
    int increaseSalesActual(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);
}
