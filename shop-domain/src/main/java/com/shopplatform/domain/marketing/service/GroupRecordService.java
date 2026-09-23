package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.GroupRecord;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.time.LocalDateTime;
import java.util.List;

public interface GroupRecordService extends TenantSafeService<GroupRecord> {

    /** 开团：创建一条 pending 记录，actual_num=1，expire=now+validHours */
    GroupRecord openGroup(Long activeId, Long leaderUserId, Long leaderOrderId, int validHours);

    /** 参团：actual_num+1，达 group_num 自动置 success。返回更新后的记录；已成团/已结束返回 null。 */
    GroupRecord joinGroup(Long recordId, int groupNum);

    /** 扫描 pending 且已过期的拼团记录（GroupExpireJob 用） */
    List<GroupRecord> listExpiredPending(LocalDateTime now);

    /** 置为失败 */
    void markFail(Long recordId);

    /**
     * 将待成团记录置为成功。用于达到人数或活动配置的“模拟成团”。
     * 只有 pending 记录允许流转，保证定时任务重复执行时幂等。
     */
    boolean markSuccess(Long recordId);

    /**
     * 释放一笔尚未付款的拼团订单占用的名额。取消订单时调用，避免未付款订单长期占用团位。
     */
    boolean leaveGroup(Long recordId);

    /** 关联砍价/拼团订单：记录 leader_order_id（开团时团长订单） */
    void setLeaderOrder(Long recordId, Long orderId);
}
