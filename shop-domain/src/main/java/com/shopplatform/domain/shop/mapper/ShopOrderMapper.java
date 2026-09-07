package com.shopplatform.domain.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.shop.entity.ShopOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShopOrderMapper extends BaseMapper<ShopOrder> {
}
