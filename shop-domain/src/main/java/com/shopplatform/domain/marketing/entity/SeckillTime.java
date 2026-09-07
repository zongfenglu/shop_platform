package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalTime;

/**
 * 秒杀场次（每日固定时段）。见文档三 §3.5、Sprint 9。
 */
@TableName("seckill_time")
public class SeckillTime extends BaseEntity {

    private Long shopId;
    private String name;
    /** 开始时分 */
    private LocalTime startTime;
    /** 结束时分 */
    private LocalTime endTime;
    private Integer sort;
    /** on / off */
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
