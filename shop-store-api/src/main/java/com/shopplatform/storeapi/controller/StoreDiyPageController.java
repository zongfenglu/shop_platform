package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.diy.entity.DiyPage;
import com.shopplatform.domain.diy.service.DiyPageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 装修页面管理（草稿/发布双版本），对应 shop-ui/store 装修页"页面列表"tab。 */
@RestController
@RequestMapping("/store/diy/pages")
public class StoreDiyPageController {

    private final DiyPageService diyPageService;

    public StoreDiyPageController(DiyPageService diyPageService) {
        this.diyPageService = diyPageService;
    }

    @GetMapping
    public Result<List<DiyPage>> list() {
        return Result.ok(diyPageService.list());
    }

    @PostMapping
    public Result<DiyPage> create(@Valid @RequestBody CreatePageRequest request) {
        return Result.ok(diyPageService.createPage(
                new DiyPageService.CreatePageCommand(request.name(), request.pageType(), request.copyFromPageId())));
    }

    @PostMapping("/from-template")
    public Result<DiyPage> applyTemplate(@Valid @RequestBody ApplyTemplateRequest request) {
        return Result.ok(diyPageService.applyTemplate(request.templateId(), request.name()));
    }

    @PutMapping("/{id}/draft")
    public Result<DiyPage> updateDraft(@PathVariable Long id, @Valid @RequestBody UpdateDraftRequest request) {
        return Result.ok(diyPageService.updateDraft(id, request.draftData()));
    }

    @PostMapping("/{id}/publish")
    public Result<DiyPage> publish(@PathVariable Long id) {
        return Result.ok(diyPageService.publish(id));
    }

    @PostMapping("/{id}/copy")
    public Result<DiyPage> copy(@PathVariable Long id, @RequestBody(required = false) CopyPageRequest request) {
        return Result.ok(diyPageService.copyPage(id, request == null ? null : request.name()));
    }

    @PutMapping("/{id}/home")
    public Result<Void> setHome(@PathVariable Long id) {
        diyPageService.setHome(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        diyPageService.delete(id);
        return Result.ok();
    }

    public record CreatePageRequest(@NotBlank String name, @NotBlank String pageType, Long copyFromPageId) {
    }

    public record ApplyTemplateRequest(Long templateId, String name) {
    }

    public record UpdateDraftRequest(@NotBlank String draftData) {
    }

    public record CopyPageRequest(String name) {
    }
}
