package com.shopplatform.domain.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 原子调整余额：balance = balance + delta，且要求扣减后不透支。
     * <p>
     * shop_id 由 TenantLineInnerInterceptor 自动追加（@Update 注解的 SQL 同样会被 JSqlParser 改写），
     * 因此这里不手写 shop_id 条件，避免与拦截器重复。返回影响行数：0 表示余额不足或会员不存在/不属于本租户。
     * <p>
     * 用 #{} 占位符参数化 delta，杜绝字符串拼接导致的 SQL 注入。
     */
    @Update("UPDATE `user` SET balance = balance + #{delta} WHERE id = #{id} AND balance + #{delta} >= 0")
    int adjustBalance(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /** 原子调整积分，扣减后不透支。 */
    @Update("UPDATE `user` SET points = points + #{delta} WHERE id = #{id} AND points + #{delta} >= 0")
    int adjustPoints(@Param("id") Long id, @Param("delta") int delta);

    /** 累加成长值。 */
    @Update("UPDATE `user` SET growth_value = growth_value + #{delta} WHERE id = #{id}")
    int addGrowth(@Param("id") Long id, @Param("delta") int delta);

    /** 累计成交金额与单数。 */
    @Update("UPDATE `user` SET pay_money = pay_money + #{money}, pay_count = pay_count + 1 WHERE id = #{id}")
    int recordPayment(@Param("id") Long id, @Param("money") BigDecimal money);

    /** 更新会员等级。 */
    @Update("UPDATE `user` SET grade_id = #{gradeId} WHERE id = #{id}")
    int updateGrade(@Param("id") Long id, @Param("gradeId") Long gradeId);

    /** 更新最后登录时间。 */
    @Update("UPDATE `user` SET last_login_time = #{time} WHERE id = #{id}")
    int updateLastLoginTime(@Param("id") Long id, @Param("time") java.time.LocalDateTime time);
}
