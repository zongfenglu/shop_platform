package com.shopplatform.domain.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.marketing.entity.PointsGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointsGoodsMapper extends BaseMapper<PointsGoods> {

    /** 原子扣库存：stock>0 时才减（0=不限），返回影响行数（0=库存不足）。 */
    @Update("UPDATE points_goods SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int decrStock(@Param("id") Long id);
}
