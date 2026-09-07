package com.shopplatform.domain.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 商品服务保障标签（如"七天无理由退换"）。见文档二 §2.2。
 * <p>
 * 命名为 GoodsServiceTag 而不是 GoodsService，避免与业务服务接口 {@code GoodsService} 撞名——
 * 数据库表名仍是 goods_service，只是 Java 类名做了区分。
 */
@TableName("goods_service")
public class GoodsServiceTag extends BaseEntity {

    private Long shopId;

    private String name;

    private String icon;

    private String intro;

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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }
}
