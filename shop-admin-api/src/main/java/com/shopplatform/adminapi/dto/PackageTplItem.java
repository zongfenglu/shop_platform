package com.shopplatform.adminapi.dto;

import com.shopplatform.domain.shop.entity.PackageTpl;

/**
 * 套餐列表项：套餐模板 + 应用商城数。
 * <p>
 * 应用商城数统计的是 {@code shop_package}（开通快照表）里 package_tpl_id 等于本模板的商城数，
 * 不是"当前套餐等于本模板"——套餐一旦开通就是快照，模板改了/删了都不影响已开通商城，
 * 所以这个数字反映的是"历史上通过这个模板开的店"，不是"现在还在用这个模板配置的店"。
 */
public record PackageTplItem(
        Long id,
        String name,
        String intro,
        String menus,
        String quota,
        String price,
        Boolean isTrial,
        Boolean isShow,
        Integer sort,
        long appliedShopCount
) {
    public static PackageTplItem of(PackageTpl tpl, long appliedShopCount) {
        return new PackageTplItem(
                tpl.getId(),
                tpl.getName(),
                tpl.getIntro(),
                tpl.getMenus(),
                tpl.getQuota(),
                tpl.getPrice(),
                tpl.getIsTrial(),
                tpl.getIsShow(),
                tpl.getSort(),
                appliedShopCount
        );
    }
}
