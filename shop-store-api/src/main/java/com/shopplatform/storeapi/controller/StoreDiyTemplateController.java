package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.diy.entity.DiyTemplate;
import com.shopplatform.domain.diy.service.DiyTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 行业模板库（平台级，只读），供商户"一键套用"生成自己的装修页。 */
@RestController
@RequestMapping("/store/diy/templates")
public class StoreDiyTemplateController {

    private final DiyTemplateService diyTemplateService;

    public StoreDiyTemplateController(DiyTemplateService diyTemplateService) {
        this.diyTemplateService = diyTemplateService;
    }

    @GetMapping
    public Result<List<DiyTemplate>> list() {
        return Result.ok(diyTemplateService.listShown());
    }
}
