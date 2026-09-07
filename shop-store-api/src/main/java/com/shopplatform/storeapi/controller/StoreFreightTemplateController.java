package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.order.entity.FreightTemplate;
import com.shopplatform.domain.order.service.FreightTemplateService;
import com.shopplatform.storeapi.dto.CreateFreightTemplateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运费模板管理。对应原型 store/settings.html 的运费模板配置项，供发布商品时选择。
 */
@RestController
@RequestMapping("/store/freight-templates")
public class StoreFreightTemplateController {

    private final FreightTemplateService freightTemplateService;

    public StoreFreightTemplateController(FreightTemplateService freightTemplateService) {
        this.freightTemplateService = freightTemplateService;
    }

    @GetMapping
    public Result<List<FreightTemplate>> list() {
        return Result.ok(freightTemplateService.list(
                Wrappers.<FreightTemplate>lambdaQuery().orderByDesc(FreightTemplate::getCreateTime)));
    }

    @PostMapping
    public Result<FreightTemplate> create(@Valid @RequestBody CreateFreightTemplateRequest request) {
        FreightTemplate template = new FreightTemplate();
        template.setName(request.name());
        template.setMethod(request.method());
        template.setRules(request.rules());
        template.setFreeRules(request.freeRules());
        freightTemplateService.save(template);
        return Result.ok(template);
    }

    @PutMapping("/{id}")
    public Result<FreightTemplate> update(@PathVariable Long id, @Valid @RequestBody CreateFreightTemplateRequest request) {
        FreightTemplate template = freightTemplateService.getByIdWithTenant(id);
        template.setName(request.name());
        template.setMethod(request.method());
        template.setRules(request.rules());
        template.setFreeRules(request.freeRules());
        freightTemplateService.updateById(template);
        return Result.ok(template);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        FreightTemplate template = freightTemplateService.getByIdWithTenant(id);
        freightTemplateService.removeById(template.getId());
        return Result.ok();
    }
}
