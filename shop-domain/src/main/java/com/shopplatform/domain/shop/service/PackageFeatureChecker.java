package com.shopplatform.domain.shop.service;

import java.util.List;

/**
 * 套餐功能位校验。基于 {@code shop_package.menus}（JSON字符串数组，精确匹配或"namespace.*"通配），
 * 只做菜单白名单判断。配额上限见 {@link PackageQuotaChecker}。
 */
public interface PackageFeatureChecker {

    boolean hasMenu(Long shopId, String menuKey);

    /** 未命中时抛出 {@link com.shopplatform.common.result.ErrorCode#PACKAGE_FEATURE_LOCKED}。 */
    void requireMenu(Long shopId, String menuKey);

    /** 返回该店铺套餐已开通的 menu 列表；无套餐或解析失败时返回空列表（不抛异常）。 */
    List<String> listMenus(Long shopId);
}
