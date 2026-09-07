package com.shopplatform.adminapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.adminapi.dto.PackageTplItem;
import com.shopplatform.adminapi.dto.PackageTplRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.shop.entity.PackageTpl;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.service.PackageTplService;
import com.shopplatform.domain.shop.service.ShopPackageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台套餐模板管理。对应原型 admin/package-list.html。
 * <p>
 * 套餐数量通常只有个位数（试用版+基础/标准/旗舰若干档），不做分页，一次性返回全部。
 */
@RestController
@RequestMapping("/admin/packages")
public class AdminPackageController {

    private final PackageTplService packageTplService;
    private final ShopPackageService shopPackageService;

    public AdminPackageController(PackageTplService packageTplService, ShopPackageService shopPackageService) {
        this.packageTplService = packageTplService;
        this.shopPackageService = shopPackageService;
    }

    @GetMapping
    public Result<List<PackageTplItem>> list() {
        List<PackageTpl> tpls = packageTplService.list(
                Wrappers.<PackageTpl>lambdaQuery().orderByAsc(PackageTpl::getSort));

        List<PackageTplItem> items = tpls.stream()
                .map(tpl -> PackageTplItem.of(tpl, countAppliedShops(tpl.getId())))
                .toList();
        return Result.ok(items);
    }

    @GetMapping("/{id}")
    public Result<PackageTplItem> detail(@PathVariable Long id) {
        PackageTpl tpl = packageTplService.getById(id);
        if (tpl == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        return Result.ok(PackageTplItem.of(tpl, countAppliedShops(id)));
    }

    @PostMapping
    public Result<PackageTplItem> create(@Valid @RequestBody PackageTplRequest request) {
        PackageTpl tpl = new PackageTpl();
        apply(tpl, request);
        packageTplService.save(tpl);
        return Result.ok(PackageTplItem.of(tpl, 0));
    }

    @PutMapping("/{id}")
    public Result<PackageTplItem> update(@PathVariable Long id, @Valid @RequestBody PackageTplRequest request) {
        PackageTpl tpl = packageTplService.getById(id);
        if (tpl == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        // 修改模板不影响已开通商城——它们读的是 shop_package 快照，见 PackageTplItem 的注释。
        apply(tpl, request);
        packageTplService.updateById(tpl);
        return Result.ok(PackageTplItem.of(tpl, countAppliedShops(id)));
    }

    /** 上架/下架：下架只影响新开店时是否可选，不影响已开通商城。 */
    @PutMapping("/{id}/show")
    public Result<Void> toggleShow(@PathVariable Long id, @RequestParam boolean show) {
        PackageTpl tpl = packageTplService.getById(id);
        if (tpl == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        tpl.setIsShow(show);
        packageTplService.updateById(tpl);
        return Result.ok();
    }

    private long countAppliedShops(Long packageTplId) {
        return shopPackageService.count(
                Wrappers.<ShopPackage>lambdaQuery().eq(ShopPackage::getPackageTplId, packageTplId));
    }

    private void apply(PackageTpl tpl, PackageTplRequest request) {
        tpl.setName(request.name());
        tpl.setIntro(request.intro());
        tpl.setMenus(request.menus());
        tpl.setQuota(request.quota());
        tpl.setPrice(request.price());
        tpl.setIsTrial(Boolean.TRUE.equals(request.isTrial()));
        tpl.setIsShow(request.isShow() == null || request.isShow());
        tpl.setSort(request.sort() == null ? 0 : request.sort());
    }
}
