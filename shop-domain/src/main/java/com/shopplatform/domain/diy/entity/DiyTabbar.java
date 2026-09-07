package com.shopplatform.domain.diy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/** 底部导航配置，每商城全局唯一一条。Sprint 14。 */
@TableName("diy_tabbar")
public class DiyTabbar extends BaseEntity {

    private Long shopId;
    /** JSON数组：[{"icon":"","activeIcon":"","text":"","path":""}]，2~5项 */
    private String items;
    /** JSON对象：{"activeColor":"","inactiveColor":"","bgColor":""} */
    private String style;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
}
