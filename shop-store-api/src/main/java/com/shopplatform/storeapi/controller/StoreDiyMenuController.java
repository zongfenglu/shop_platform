package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.shop.service.PackageFeatureChecker;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前商城套餐已开通的 menu 列表，供装修编辑器判断组件是否锁定。 */
@RestController
@RequestMapping("/store/diy/menus")
public class StoreDiyMenuController {

    private final PackageFeatureChecker packageFeatureChecker;

    public StoreDiyMenuController(PackageFeatureChecker packageFeatureChecker) {
        this.packageFeatureChecker = packageFeatureChecker;
    }

    @GetMapping
    public Result<List<String>> list() {
        return Result.ok(packageFeatureChecker.listMenus(TenantContext.getRequired()));
    }
}
