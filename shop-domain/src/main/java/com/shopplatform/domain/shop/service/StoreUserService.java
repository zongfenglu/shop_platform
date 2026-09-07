package com.shopplatform.domain.shop.service;

import com.shopplatform.framework.mybatis.TenantSafeService;
import com.shopplatform.domain.shop.entity.StoreUser;

public interface StoreUserService extends TenantSafeService<StoreUser> {

    /** 商户后台登录：按用户名查找账号并校验密码，与租户上下文无关（登录时还不知道 shopId，需要跨租户按用户名找账号）。 */
    StoreUser findByUsername(Long shopId, String username);

    /** 平台免密登录用：优先取超级店主，没有则取该店第一个启用账号。登录前无 TenantContext。 */
    StoreUser findImpersonationTarget(Long shopId);

    /** 平台重置店主密码用：取超级店主，不论账号是否停用。登录前无 TenantContext。 */
    StoreUser findSuperOwner(Long shopId);

    void touchLastLogin(Long userId);
}
