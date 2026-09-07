package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDate;

/**
 * 秒杀/限时折扣活动。见文档三 §3.5、Sprint 9。
 * <p>
 * time_ids 为空（null 或空数组）= 限时折扣：在 start_date..end_date 全天有效，仅换价、走普通库存、无限购预扣；
 * time_ids 非空 = 秒杀：需当前时间落在某个 seckill_time 时段内，限量 + 限购 + Redis 预扣。
 */
@TableName("seckill_active")
public class SeckillActive extends BaseEntity {

    private Long shopId;
    private String name;
    /** 场次 id 数组（JSON 字符串）；空/null = 限时折扣 */
    private String timeIds;
    private LocalDate startDate;
    private LocalDate endDate;
    /** on / off */
    private String status;
    private String remark;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTimeIds() { return timeIds; }
    public void setTimeIds(String timeIds) { this.timeIds = timeIds; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
