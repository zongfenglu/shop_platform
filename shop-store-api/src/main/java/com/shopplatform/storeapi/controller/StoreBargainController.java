package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.entity.BargainActive;
import com.shopplatform.domain.marketing.service.BargainActiveService;
import com.shopplatform.storeapi.dto.SaveBargainActiveRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户后台砍价活动配置 CRUD。对应原型 docs/prototype/store/marketing-bargain.html。
 */
@RestController
@RequestMapping("/store/bargain")
public class StoreBargainController {

    private final BargainActiveService bargainActiveService;

    public StoreBargainController(BargainActiveService bargainActiveService) {
        this.bargainActiveService = bargainActiveService;
    }

    @GetMapping("/actives")
    public Result<List<BargainActive>> list() {
        return Result.ok(bargainActiveService.listAll());
    }

    @PostMapping("/actives")
    public Result<BargainActive> create(@Valid @RequestBody SaveBargainActiveRequest req) {
        BargainActive a = new BargainActive();
        apply(req, a);
        bargainActiveService.save(a);
        return Result.ok(a);
    }

    @PutMapping("/actives/{id}")
    public Result<BargainActive> update(@PathVariable Long id, @Valid @RequestBody SaveBargainActiveRequest req) {
        BargainActive a = bargainActiveService.getByIdWithTenant(id);
        apply(req, a);
        bargainActiveService.updateById(a);
        return Result.ok(a);
    }

    @DeleteMapping("/actives/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bargainActiveService.getByIdWithTenant(id);
        bargainActiveService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveBargainActiveRequest req, BargainActive a) {
        a.setGoodsId(req.goodsId());
        a.setFloorPrice(req.floorPrice());
        a.setValidHours(req.validHours());
        a.setHelpLimit(req.helpLimit() == null ? 0 : req.helpLimit());
        a.setStartTime(req.startTime());
        a.setEndTime(req.endTime());
        a.setStatus(req.status() == null ? "on" : req.status());
    }
}
