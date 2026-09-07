package com.shopplatform.domain.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.marketing.entity.BargainRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface BargainRecordMapper extends BaseMapper<BargainRecord> {

    /** 助力砍价：current_price 递减 cutAmount，不低于 floorPrice；help_count+1。返回 0 表示已结束/已到底价。 */
    @Update("UPDATE bargain_record SET current_price = current_price - #{cutAmount}, help_count = help_count + 1 " +
            "WHERE id = #{id} AND status = 'ongoing' AND current_price - #{cutAmount} >= #{floorPrice}")
    int helpCut(@Param("id") Long id, @Param("cutAmount") BigDecimal cutAmount, @Param("floorPrice") BigDecimal floorPrice);
}
