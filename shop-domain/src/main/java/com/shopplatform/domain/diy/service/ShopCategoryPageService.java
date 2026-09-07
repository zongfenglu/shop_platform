package com.shopplatform.domain.diy.service;

import com.shopplatform.domain.diy.entity.ShopCategoryPage;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface ShopCategoryPageService extends TenantSafeService<ShopCategoryPage> {

    /** 当前租户的分类页模板；没有则写入默认「一级分类小图」。 */
    ShopCategoryPage getOrCreate();

    ShopCategoryPage saveStyle(String style, String shareTitle);
}
