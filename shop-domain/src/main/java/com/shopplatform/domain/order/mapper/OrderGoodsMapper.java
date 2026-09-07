package com.shopplatform.domain.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.order.entity.OrderGoods;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderGoodsMapper extends BaseMapper<OrderGoods> {
}
