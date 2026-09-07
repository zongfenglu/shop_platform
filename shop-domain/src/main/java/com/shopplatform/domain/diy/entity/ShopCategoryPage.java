package com.shopplatform.domain.diy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 分类页模板。每租户一条，控制 C 端分类 Tab 的版式。
 */
@TableName("shop_category_page")
public class ShopCategoryPage extends BaseEntity {

    public static final String STYLE_LEVEL1_LARGE = "level1_large";
    public static final String STYLE_LEVEL1_SMALL = "level1_small";
    public static final String STYLE_LEVEL2 = "level2";

    private Long shopId;

    /** level1_large / level1_small / level2 */
    private String style;

    private String shareTitle;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getShareTitle() {
        return shareTitle;
    }

    public void setShareTitle(String shareTitle) {
        this.shareTitle = shareTitle;
    }
}
