package com.shopplatform.domain.shop.service;

/**
 * 套餐配额校验。上限来自当前生效 {@code shop_package.quota} 快照。
 * {@code max < 0} 表示不限；缺套餐或缺字段视为不限，避免未挂套餐的旧店被误伤。
 */
public interface PackageQuotaChecker {

    void requireGoods();

    void requireStaff();

    void requireStore();

    void requireDiyPage();
}
