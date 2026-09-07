package com.shopplatform.domain.shop.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.service.PackageFeatureChecker;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.ShopService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 只做 {@code shop_package.menus} 白名单判断，不做配额（quota）校验——
 * quota 机制全代码库尚未实现，非本次（Sprint 14 装修）引入范围。
 */
@Service
public class PackageFeatureCheckerImpl implements PackageFeatureChecker {

    private final ShopService shopService;
    private final ShopPackageService shopPackageService;
    private final ObjectMapper objectMapper;

    public PackageFeatureCheckerImpl(ShopService shopService, ShopPackageService shopPackageService,
                                      ObjectMapper objectMapper) {
        this.shopService = shopService;
        this.shopPackageService = shopPackageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean hasMenu(Long shopId, String menuKey) {
        List<String> menus = listMenus(shopId);
        String namespace = menuKey.contains(".") ? menuKey.substring(0, menuKey.indexOf('.')) + ".*" : null;
        return menus.contains(menuKey) || (namespace != null && menus.contains(namespace));
    }

    @Override
    public void requireMenu(Long shopId, String menuKey) {
        if (!hasMenu(shopId, menuKey)) {
            throw new BusinessException(ErrorCode.PACKAGE_FEATURE_LOCKED);
        }
    }

    @Override
    public List<String> listMenus(Long shopId) {
        Shop shop = shopService.getByIdWithTenant(shopId);
        if (shop.getPackageId() == null) {
            return Collections.emptyList();
        }
        ShopPackage shopPackage;
        try {
            shopPackage = shopPackageService.getByIdWithTenant(shop.getPackageId());
        } catch (TenantAccessDeniedException e) {
            return Collections.emptyList();
        }
        if (shopPackage.getMenus() == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(shopPackage.getMenus(), new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
