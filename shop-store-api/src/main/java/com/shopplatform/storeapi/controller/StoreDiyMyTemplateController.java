package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.diy.entity.DiyMyTemplate;
import com.shopplatform.domain.diy.service.DiyMyTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 商户私有装修模板（"存为模板"功能），与平台级只读 /store/diy/templates 语义分离。 */
@RestController
@RequestMapping("/store/diy/my-templates")
public class StoreDiyMyTemplateController {

    private final DiyMyTemplateService diyMyTemplateService;

    public StoreDiyMyTemplateController(DiyMyTemplateService diyMyTemplateService) {
        this.diyMyTemplateService = diyMyTemplateService;
    }

    @GetMapping
    public Result<List<DiyMyTemplate>> list() {
        return Result.ok(diyMyTemplateService.listMine());
    }

    @PostMapping
    public Result<DiyMyTemplate> save(@Valid @RequestBody SaveRequest request) {
        return Result.ok(diyMyTemplateService.save(request.name(), request.pageData()));
    }

    public record SaveRequest(@NotBlank String name, @NotBlank String pageData) {
    }
}
