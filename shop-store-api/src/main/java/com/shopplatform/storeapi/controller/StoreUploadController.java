package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.diy.entity.DiyMaterial;
import com.shopplatform.domain.diy.entity.DiyMaterialGroup;
import com.shopplatform.domain.diy.service.DiyMaterialGroupService;
import com.shopplatform.domain.diy.service.DiyMaterialService;
import com.shopplatform.domain.file.service.StorageService;
import com.shopplatform.framework.tenant.TenantContext;
import com.shopplatform.storeapi.dto.MaterialGroupRequest;
import com.shopplatform.storeapi.dto.MoveMaterialRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片上传与素材库。装修、商品、营销配图都走这里，按分组归档后可复用。
 */
@RestController
@RequestMapping("/store")
public class StoreUploadController {

    private final StorageService storageService;
    private final DiyMaterialService diyMaterialService;
    private final DiyMaterialGroupService diyMaterialGroupService;

    public StoreUploadController(
            StorageService storageService,
            DiyMaterialService diyMaterialService,
            DiyMaterialGroupService diyMaterialGroupService) {
        this.storageService = storageService;
        this.diyMaterialService = diyMaterialService;
        this.diyMaterialGroupService = diyMaterialGroupService;
    }

    @PostMapping("/upload/image")
    public Result<DiyMaterial> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long groupId) {
        StorageService.StoredFile stored = storageService.storeImage(file, TenantContext.getRequired());
        return Result.ok(diyMaterialService.record(stored, groupId));
    }

    @GetMapping("/materials")
    public Result<IPage<DiyMaterial>> pageMaterials(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "24") int pageSize,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "false") boolean ungrouped,
            @RequestParam(required = false) String keyword) {
        return Result.ok(diyMaterialService.pageMine(pageNum, Math.min(pageSize, 100), groupId, ungrouped, keyword));
    }

    @PutMapping("/materials/{id}/group")
    public Result<Void> moveMaterial(@PathVariable Long id, @RequestBody MoveMaterialRequest request) {
        diyMaterialService.moveToGroup(id, request == null ? null : request.groupId());
        return Result.ok();
    }

    @DeleteMapping("/materials/{id}")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        diyMaterialService.remove(id);
        return Result.ok();
    }

    @GetMapping("/material-groups")
    public Result<Map<String, Object>> listGroups() {
        List<DiyMaterialGroup> groups = diyMaterialGroupService.listMine();
        Map<Long, Long> counts = diyMaterialService.countByGroup();
        long ungrouped = counts.getOrDefault(null, 0L);
        long grouped = 0;
        List<Map<String, Object>> items = new ArrayList<>();
        for (DiyMaterialGroup group : groups) {
            long count = counts.getOrDefault(group.getId(), 0L);
            grouped += count;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", group.getId());
            item.put("name", group.getName());
            item.put("sort", group.getSort());
            item.put("count", count);
            items.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", ungrouped + grouped);
        data.put("ungrouped", ungrouped);
        data.put("groups", items);
        return Result.ok(data);
    }

    @PostMapping("/material-groups")
    public Result<DiyMaterialGroup> createGroup(@Valid @RequestBody MaterialGroupRequest request) {
        return Result.ok(diyMaterialGroupService.create(request.name()));
    }

    @PutMapping("/material-groups/{id}")
    public Result<Void> renameGroup(@PathVariable Long id, @Valid @RequestBody MaterialGroupRequest request) {
        diyMaterialGroupService.rename(id, request.name());
        return Result.ok();
    }

    @DeleteMapping("/material-groups/{id}")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        diyMaterialGroupService.removeGroup(id);
        return Result.ok();
    }
}
