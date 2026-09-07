package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 分销设置管理。对应原型 store/distribution.html 分销设置。 */
@RestController
@RequestMapping("/store/dealer/settings")
public class StoreDealerSettingController {

    private final DealerSettingService dealerSettingService;

    public StoreDealerSettingController(DealerSettingService dealerSettingService) {
        this.dealerSettingService = dealerSettingService;
    }

    @GetMapping
    public Result<DealerSetting> get() {
        return Result.ok(dealerSettingService.getOrCreate());
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody DealerSetting setting) {
        dealerSettingService.saveSetting(setting);
        return Result.ok();
    }
}
