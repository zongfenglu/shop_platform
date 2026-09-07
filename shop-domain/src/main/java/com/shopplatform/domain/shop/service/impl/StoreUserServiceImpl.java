package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.mapper.StoreUserMapper;
import com.shopplatform.domain.shop.service.StoreUserService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StoreUserServiceImpl extends ServiceImpl<StoreUserMapper, StoreUser> implements StoreUserService {

    @Override
    public StoreUser findByUsername(Long shopId, String username) {
        // 登录场景：TenantContext 尚未建立（JWT 还没发出去），但商户后台域名本身已经隐含了 shopId
        // （由 Host 头在鉴权前置逻辑中解析得到，见 shop-store-api 的登录 Controller）。
        // 这里显式用 ignoreTenant + 手动拼 shop_id 条件，而不是依赖自动拦截器，
        // 因为自动拦截器要求 TenantContext 已经 set 过，登录这个动作恰好发生在"设置 TenantContext 之前"。
        return TenantContext.ignoreTenant(() -> this.getOne(
                Wrappers.<StoreUser>lambdaQuery()
                        .eq(StoreUser::getShopId, shopId)
                        .eq(StoreUser::getUsername, username)));
    }

    @Override
    public StoreUser findImpersonationTarget(Long shopId) {
        return TenantContext.ignoreTenant(() -> {
            StoreUser owner = this.getOne(Wrappers.<StoreUser>lambdaQuery()
                    .eq(StoreUser::getShopId, shopId)
                    .eq(StoreUser::getIsSuperOwner, true)
                    .eq(StoreUser::getStatus, 1)
                    .last("LIMIT 1"));
            if (owner != null) {
                return owner;
            }
            return this.getOne(Wrappers.<StoreUser>lambdaQuery()
                    .eq(StoreUser::getShopId, shopId)
                    .eq(StoreUser::getStatus, 1)
                    .orderByAsc(StoreUser::getId)
                    .last("LIMIT 1"));
        });
    }

    @Override
    public StoreUser findSuperOwner(Long shopId) {
        return TenantContext.ignoreTenant(() -> this.getOne(Wrappers.<StoreUser>lambdaQuery()
                .eq(StoreUser::getShopId, shopId)
                .eq(StoreUser::getIsSuperOwner, true)
                .last("LIMIT 1")));
    }

    @Override
    public void touchLastLogin(Long userId) {
        TenantContext.ignoreTenant(() -> this.update(Wrappers.<StoreUser>lambdaUpdate()
                .eq(StoreUser::getId, userId)
                .set(StoreUser::getLastLoginTime, LocalDateTime.now())));
    }
}
