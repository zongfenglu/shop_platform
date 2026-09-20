package com.shopplatform.domain.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 将同一租户内一个会员的关联数据转移到另一个会员。
     * table/column 只由领域服务传入固定白名单，不能接收外部请求参数。
     */
    @Update("UPDATE ${table} SET ${column} = #{targetId} "
            + "WHERE shop_id = #{shopId} AND ${column} = #{sourceId}")
    int moveUserReference(@Param("table") String table,
                          @Param("column") String column,
                          @Param("sourceId") Long sourceId,
                          @Param("targetId") Long targetId,
                          @Param("shopId") Long shopId);

    /** 主账号已有同日签到时，删除被合并账号的重复签到记录。 */
    @Delete("DELETE FROM sign_record WHERE id IN ("
            + "SELECT duplicate_ids.id FROM ("
            + "SELECT source.id FROM sign_record source "
            + "JOIN sign_record target ON target.shop_id = source.shop_id "
            + "AND target.user_id = #{targetId} AND target.sign_date = source.sign_date "
            + "WHERE source.shop_id = #{shopId} AND source.user_id = #{sourceId}"
            + ") duplicate_ids)")
    int deleteDuplicateSignRecords(@Param("sourceId") Long sourceId,
                                   @Param("targetId") Long targetId,
                                   @Param("shopId") Long shopId);

    /** 主账号已有同一砍价活动记录时，删除被合并账号的重复记录。 */
    @Delete("DELETE FROM bargain_record WHERE id IN ("
            + "SELECT duplicate_ids.id FROM ("
            + "SELECT source.id FROM bargain_record source "
            + "JOIN bargain_record target ON target.shop_id = source.shop_id "
            + "AND target.user_id = #{targetId} AND target.active_id = source.active_id "
            + "WHERE source.shop_id = #{shopId} AND source.user_id = #{sourceId}"
            + ") duplicate_ids)")
    int deleteDuplicateBargainRecords(@Param("sourceId") Long sourceId,
                                      @Param("targetId") Long targetId,
                                      @Param("shopId") Long shopId);

    /** 将两个账号的分销订单归并到主账号的分销商记录。 */
    @Update("UPDATE dealer_order o "
            + "JOIN dealer_user source ON source.shop_id = o.shop_id AND source.id = o.dealer_user_id "
            + "JOIN dealer_user target ON target.shop_id = o.shop_id AND target.user_id = #{targetId} "
            + "SET o.dealer_user_id = target.id "
            + "WHERE o.shop_id = #{shopId} AND source.user_id = #{sourceId}")
    int moveDealerOrders(@Param("sourceId") Long sourceId,
                         @Param("targetId") Long targetId,
                         @Param("shopId") Long shopId);

    /** 将分销提现记录归并到主账号的分销商记录。 */
    @Update("UPDATE dealer_withdraw w "
            + "JOIN dealer_user source ON source.shop_id = w.shop_id AND source.id = w.dealer_user_id "
            + "JOIN dealer_user target ON target.shop_id = w.shop_id AND target.user_id = #{targetId} "
            + "SET w.dealer_user_id = target.id, w.user_id = #{targetId} "
            + "WHERE w.shop_id = #{shopId} AND source.user_id = #{sourceId}")
    int moveDealerWithdraws(@Param("sourceId") Long sourceId,
                            @Param("targetId") Long targetId,
                            @Param("shopId") Long shopId);

    /** 主账号已有分销资料时，累加被合并账号的佣金汇总。 */
    @Update("UPDATE dealer_user target "
            + "JOIN dealer_user source ON source.shop_id = target.shop_id "
            + "AND source.user_id = #{sourceId} "
            + "SET target.total_commission = target.total_commission + source.total_commission, "
            + "target.available_commission = target.available_commission + source.available_commission, "
            + "target.frozen_commission = target.frozen_commission + source.frozen_commission "
            + "WHERE target.shop_id = #{shopId} AND target.user_id = #{targetId}")
    int mergeDealerTotals(@Param("sourceId") Long sourceId,
                          @Param("targetId") Long targetId,
                          @Param("shopId") Long shopId);

    /** 删除已转移到主账号分销资料的重复分销档案。 */
    @Delete("DELETE source FROM dealer_user source "
            + "JOIN dealer_user target ON target.shop_id = source.shop_id "
            + "AND target.user_id = #{targetId} "
            + "WHERE source.shop_id = #{shopId} AND source.user_id = #{sourceId}")
    int deleteMergedDealerUser(@Param("sourceId") Long sourceId,
                               @Param("targetId") Long targetId,
                               @Param("shopId") Long shopId);

    /** 主账号没有分销档案时，直接把被合并账号的档案改挂到主账号。 */
    @Update("UPDATE dealer_user source "
            + "LEFT JOIN dealer_user target ON target.shop_id = source.shop_id "
            + "AND target.user_id = #{targetId} "
            + "SET source.user_id = #{targetId} "
            + "WHERE source.shop_id = #{shopId} AND source.user_id = #{sourceId} "
            + "AND target.id IS NULL")
    int moveDealerUserWhenTargetMissing(@Param("sourceId") Long sourceId,
                                        @Param("targetId") Long targetId,
                                        @Param("shopId") Long shopId);

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
