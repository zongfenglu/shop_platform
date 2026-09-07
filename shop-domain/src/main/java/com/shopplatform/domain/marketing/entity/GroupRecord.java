package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 拼团记录（一个团一条）。见 Sprint 10。
 * 团长开团时创建 status=pending、actual_num=1；每有人参团 actual_num+1；
 * actual_num &gt;= group_active.group_num 时置 success；超时未成团由 GroupExpireJob 置 fail 并原路退款。
 */
@TableName("group_record")
public class GroupRecord extends BaseEntity {

    private Long shopId;
    private Long activeId;
    private Long leaderUserId;
    private Long leaderOrderId;
    /** pending / success / fail */
    private String status;
    private Integer actualNum;
    private LocalDateTime expireTime;
    private LocalDateTime successTime;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getActiveId() { return activeId; }
    public void setActiveId(Long activeId) { this.activeId = activeId; }
    public Long getLeaderUserId() { return leaderUserId; }
    public void setLeaderUserId(Long leaderUserId) { this.leaderUserId = leaderUserId; }
    public Long getLeaderOrderId() { return leaderOrderId; }
    public void setLeaderOrderId(Long leaderOrderId) { this.leaderOrderId = leaderOrderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getActualNum() { return actualNum; }
    public void setActualNum(Integer actualNum) { this.actualNum = actualNum; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getSuccessTime() { return successTime; }
    public void setSuccessTime(LocalDateTime successTime) { this.successTime = successTime; }
}
