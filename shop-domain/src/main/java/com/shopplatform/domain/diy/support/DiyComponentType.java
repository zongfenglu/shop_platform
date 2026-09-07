package com.shopplatform.domain.diy.support;

import java.util.Arrays;
import java.util.Optional;

/**
 * 装修组件类型白名单。requiredMenu 非空的组件需要商户套餐 menus 包含对应功能位
 * （见 {@link com.shopplatform.domain.shop.service.PackageFeatureChecker}），
 * 键名与 V4__platform_seed_data.sql 种子的 package_tpl.menus 字符串完全对应。
 */
public enum DiyComponentType {

    SEARCH("search", null),
    BANNER("banner", null),
    IMAGE_GROUP("imageGroup", null),
    IMAGE_WINDOW("imageWindow", null),
    VIDEO("video", null),
    ARTICLE("article", null),
    NEWS("news", null),
    NOTICE("notice", null),
    NAV_BAR("navBar", null),
    GOODS("goods", null),
    STORE("store", "store.offline"),
    DIVIDER("divider", null),
    FOLLOW_MP("followMp", null),
    RICH_TEXT("richText", null),
    BLANK("blank", null),
    CUSTOMER_SERVICE("customerService", null),
    COUPON("coupon", "marketing.coupon"),
    SECKILL("seckill", "marketing.seckill"),
    GROUP("group", "marketing.group"),
    BARGAIN("bargain", "marketing.bargain");

    private final String type;
    private final String requiredMenu;

    DiyComponentType(String type, String requiredMenu) {
        this.type = type;
        this.requiredMenu = requiredMenu;
    }

    public String getType() {
        return type;
    }

    public String getRequiredMenu() {
        return requiredMenu;
    }

    public static Optional<DiyComponentType> fromType(String type) {
        return Arrays.stream(values()).filter(v -> v.type.equals(type)).findFirst();
    }
}
