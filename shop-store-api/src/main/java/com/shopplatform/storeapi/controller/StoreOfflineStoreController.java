package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.shop.service.PackageQuotaChecker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** 门店管理，对应 docs/prototype/store/store-offline.html。 */
@RestController
@RequestMapping("/store/offline-stores")
public class StoreOfflineStoreController {

    private final OfflineStoreService offlineStoreService;
    private final PackageQuotaChecker packageQuotaChecker;

    public StoreOfflineStoreController(OfflineStoreService offlineStoreService,
                                       PackageQuotaChecker packageQuotaChecker) {
        this.offlineStoreService = offlineStoreService;
        this.packageQuotaChecker = packageQuotaChecker;
    }

    @GetMapping
    public Result<List<OfflineStore>> list() {
        return Result.ok(offlineStoreService.list(
                Wrappers.<OfflineStore>lambdaQuery().orderByDesc(OfflineStore::getCreateTime)));
    }

    @PostMapping
    public Result<OfflineStore> create(@Valid @RequestBody SaveRequest request) {
        packageQuotaChecker.requireStore();
        OfflineStore store = new OfflineStore();
        apply(store, request);
        store.setStatus("enabled");
        offlineStoreService.save(store);
        return Result.ok(store);
    }

    @PutMapping("/{id}")
    public Result<OfflineStore> update(@PathVariable Long id, @Valid @RequestBody SaveRequest request) {
        OfflineStore store = offlineStoreService.getByIdWithTenant(id);
        apply(store, request);
        offlineStoreService.updateById(store);
        return Result.ok(store);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        OfflineStore store = offlineStoreService.getByIdWithTenant(id);
        store.setStatus(request.status());
        offlineStoreService.updateById(store);
        return Result.ok();
    }

    private void apply(OfflineStore store, SaveRequest request) {
        store.setName(request.name().trim());
        store.setLogo(request.logo());
        store.setPhone(request.phone());
        store.setRegion(request.region());
        store.setDetail(request.detail());
        store.setLongitude(request.longitude());
        store.setLatitude(request.latitude());
        store.setBusinessHours(request.businessHours());
    }

    public record SaveRequest(
            @NotBlank @Size(max = 64) String name,
            String logo,
            @Size(max = 20) String phone,
            String region,
            String detail,
            BigDecimal longitude,
            BigDecimal latitude,
            String businessHours) {
    }

    public record StatusRequest(@NotBlank @Size(max = 10) String status) {
    }
}
