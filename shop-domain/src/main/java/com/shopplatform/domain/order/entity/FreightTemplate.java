package com.shopplatform.domain.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 运费模板。见文档三 §3.6、§4 FreightHandler。
 */
@TableName("freight_template")
public class FreightTemplate extends BaseEntity {

    private Long shopId;

    private String name;

    /** weight按重量 / count按件数 / volume按体积 */
    private String method;

    /** JSON：阶梯计费规则 */
    private String rules;

    /** JSON：包邮规则 */
    private String freeRules;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getFreeRules() {
        return freeRules;
    }

    public void setFreeRules(String freeRules) {
        this.freeRules = freeRules;
    }
}
