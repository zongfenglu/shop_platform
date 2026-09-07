package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 满减/满件折规则。见文档三 §3.5。
 * type=money：rules 为 [{"threshold":100.00,"reduce":10.00}, ...]，取满足门槛中最大的一档。
 * type=count：rules 为 [{"threshold":2,"discount":0.90}, ...]，满 N 件打 M 折。
 */
@TableName("full_reduce_rule")
public class FullReduceRule extends BaseEntity {

    private Long shopId;
    private String name;
    /** money / count */
    private String type;
    private String rules;
    private Integer freeExpress;
    /** on / off */
    private String status;
    private Integer sort;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }
    public Integer getFreeExpress() { return freeExpress; }
    public void setFreeExpress(Integer freeExpress) { this.freeExpress = freeExpress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
