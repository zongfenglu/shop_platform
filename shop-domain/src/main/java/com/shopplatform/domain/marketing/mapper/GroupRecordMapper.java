package com.shopplatform.domain.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.marketing.entity.GroupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GroupRecordMapper extends BaseMapper<GroupRecord> {

    /** 参团：actual_num+1，带上限校验（active 的 group_num 由调用方传入）。返回 0 表示已成团/已结束。 */
    @Update("UPDATE group_record SET actual_num = actual_num + #{qty} " +
            "WHERE id = #{id} AND status = 'pending' AND actual_num + #{qty} <= #{groupNum}")
    int joinGroup(@Param("id") Long id, @Param("qty") int qty, @Param("groupNum") int groupNum);
}
