package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.member.entity.RechargePlan;
import com.shopplatform.domain.member.service.RechargePlanService;
import com.shopplatform.storeapi.dto.SaveRechargePlanRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 余额充值方案管理。对应原型 store 会员资产/充值相关配置。 */
@RestController
@RequestMapping("/store/recharge-plans")
public class StoreRechargePlanController {

    private final RechargePlanService rechargePlanService;

    public StoreRechargePlanController(RechargePlanService rechargePlanService) {
        this.rechargePlanService = rechargePlanService;
    }

    @GetMapping
    public Result<List<RechargePlan>> list() {
        return Result.ok(rechargePlanService.list());
    }

    @PostMapping
    public Result<RechargePlan> create(@Valid @RequestBody SaveRechargePlanRequest req) {
        RechargePlan plan = new RechargePlan();
        apply(req, plan);
        rechargePlanService.save(plan);
        return Result.ok(plan);
    }

    @PutMapping("/{id}")
    public Result<RechargePlan> update(@PathVariable Long id, @Valid @RequestBody SaveRechargePlanRequest req) {
        RechargePlan plan = rechargePlanService.getByIdWithTenant(id);
        apply(req, plan);
        rechargePlanService.updateById(plan);
        return Result.ok(plan);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rechargePlanService.getByIdWithTenant(id);
        rechargePlanService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveRechargePlanRequest req, RechargePlan plan) {
        plan.setMoney(req.money());
        plan.setGiftMoney(req.giftMoney());
        plan.setGiftPoints(req.giftPoints());
        plan.setIsShow(req.isShow() == null ? 1 : req.isShow());
        plan.setSort(req.sort() == null ? 0 : req.sort());
    }
}
