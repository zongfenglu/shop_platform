package com.shopplatform.domain.dealer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.dealer.entity.DealerOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface DealerOrderMapper extends BaseMapper<DealerOrder> {

    /** 原子增加可提现佣金（settle 时调用），同时累加 total。 */
    @Update("UPDATE dealer_user SET available_commission = available_commission + #{amount}, " +
            "total_commission = total_commission + #{amount}, frozen_commission = frozen_commission - #{amount} " +
            "WHERE id = #{dealerId} AND frozen_commission >= #{amount}")
    int settleCommission(@Param("dealerId") Long dealerId, @Param("amount") BigDecimal amount);
}
