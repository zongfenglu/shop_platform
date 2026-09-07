package com.shopplatform.adminapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.adminapi.dto.*;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.platform.entity.PlatformMenu;
import com.shopplatform.domain.platform.entity.PlatformRole;
import com.shopplatform.domain.platform.entity.PlatformUser;
import com.shopplatform.domain.platform.mapper.PlatformMenuMapper;
import com.shopplatform.domain.platform.service.PlatformUserService;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 平台账号管理。
 */
@RestController
@RequestMapping("/admin/platform-users")
public class AdminPlatformUserController {

    private final PlatformUserService platformUserService;
    private final PlatformMenuMapper platformMenuMapper;

    public AdminPlatformUserController(PlatformUserService platformUserService, PlatformMenuMapper platformMenuMapper) {
        this.platformUserService = platformUserService;
        this.platformMenuMapper = platformMenuMapper;
    }

    @GetMapping
    public Result<PlatformUserPageResponse> page(PlatformUserListQuery query) {
        var wrapper = Wrappers.<PlatformUser>lambdaQuery();
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();
            wrapper.and(w -> w.like(PlatformUser::getUsername, keyword)
                    .or().like(PlatformUser::getRealName, keyword)
                    .or().like(PlatformUser::getMobile, keyword));
        }
        if (query.status() != null) {
            wrapper.eq(PlatformUser::getStatus, query.status());
        }
        wrapper.orderByDesc(PlatformUser::getCreateTime);

        Page<PlatformUser> page = platformUserService.page(
                new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault()), wrapper);

        List<PlatformUserItem> records = page.getRecords().stream()
                .map(this::toItem)
                .toList();
        PlatformUserSummary summary = buildSummary();
        return Result.ok(new PlatformUserPageResponse(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), summary));
    }

    @GetMapping("/roles")
    public Result<List<PlatformRole>> roles() {
        return Result.ok(platformUserService.listRoles());
    }

    @GetMapping("/roles/{roleId}/menus")
    public Result<List<Long>> roleMenus(@PathVariable Long roleId) {
        return Result.ok(platformUserService.getRoleMenuIds(roleId));
    }

    @PutMapping("/roles/{roleId}/menus")
    public Result<Void> saveRoleMenus(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        platformUserService.saveRoleMenus(roleId, body.get("menuIds"));
        return Result.ok();
    }

    @GetMapping("/menus")
    public Result<List<PlatformMenu>> menus() {
        return Result.ok(platformMenuMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<PlatformMenu>lambdaQuery().orderByAsc(PlatformMenu::getSort)));
    }

    @PostMapping
    public Result<PlatformUserItem> create(@Valid @RequestBody SavePlatformUserRequest req) {
        PlatformUser user = platformUserService.create(
                req.username(), req.password(), req.realName(), req.mobile(), req.status(), req.roleId());
        return Result.ok(toItem(user));
    }

    @PutMapping("/{id}")
    public Result<PlatformUserItem> update(@PathVariable Long id, @Valid @RequestBody SavePlatformUserRequest req) {
        PlatformUser user = platformUserService.update(
                id, req.username(), req.realName(), req.mobile(), req.status(), req.roleId());
        return Result.ok(toItem(user));
    }

    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.isBlank()) {
            password = "123456";
        }
        platformUserService.resetPassword(id, password);
        return Result.ok();
    }

    @PostMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        platformUserService.toggleStatus(id);
        return Result.ok();
    }

    private PlatformUserSummary buildSummary() {
        long total = platformUserService.count();
        long active = platformUserService.count(Wrappers.<PlatformUser>lambdaQuery().eq(PlatformUser::getStatus, 1));
        long disabled = platformUserService.count(Wrappers.<PlatformUser>lambdaQuery().eq(PlatformUser::getStatus, 0));
        return new PlatformUserSummary(total, active, disabled);
    }

    private PlatformUserItem toItem(PlatformUser user) {
        Long roleId = platformUserService.getRoleId(user.getId());
        // 简单映射 roleId → roleName（一次性查所有角色做映射）
        String roleName = "—";
        if (roleId != null) {
            List<PlatformRole> roles = platformUserService.listRoles();
            roleName = roles.stream()
                    .filter(r -> r.getId().equals(roleId))
                    .map(PlatformRole::getName)
                    .findFirst()
                    .orElse("—");
        }
        if (user.getUsername() != null && user.getUsername().equals("admin")) {
            roleName = "超管";
        }
        return new PlatformUserItem(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getMobile(),
                roleName,
                roleId,
                user.getStatus(),
                user.getLastLoginTime(),
                user.getCreateTime()
        );
    }
}
