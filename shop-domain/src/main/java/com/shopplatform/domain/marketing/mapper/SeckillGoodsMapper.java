package com.shopplatform.domain.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /** 秒杀下单成功后累加已售，带 seckill_num 上限校验（0=不限）。返回 0 表示已售罄/状态异常。 */
    @Update("UPDATE seckill_goods SET sold = sold + #{qty} WHERE id = #{id} AND status = 'on' AND (seckill_num = 0 OR sold + #{qty} <= seckill_num)")
    int incrSold(@Param("id") Long id, @Param("qty") int qty);
}
