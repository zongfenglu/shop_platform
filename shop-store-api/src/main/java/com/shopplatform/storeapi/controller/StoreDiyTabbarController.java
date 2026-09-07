package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.diy.entity.DiyTabbar;
import com.shopplatform.domain.diy.service.DiyTabbarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

/** 底部导航配置，每商城全局唯一一条。 */
@RestController
@RequestMapping("/store/diy/tabbar")
public class StoreDiyTabbarController {

    private final DiyTabbarService diyTabbarService;

    public StoreDiyTabbarController(DiyTabbarService diyTabbarService) {
        this.diyTabbarService = diyTabbarService;
    }

    @GetMapping
    public Result<DiyTabbar> get() {
        return Result.ok(diyTabbarService.getOrCreateDefault());
    }

    @PutMapping
    public Result<DiyTabbar> save(@Valid @RequestBody SaveTabbarRequest request) {
        return Result.ok(diyTabbarService.save(request.items(), request.style()));
    }

    public record SaveTabbarRequest(@NotBlank String items, String style) {
    }
}
