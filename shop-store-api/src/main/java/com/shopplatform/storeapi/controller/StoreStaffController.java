package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.shop.entity.StoreRole;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.service.PackageQuotaChecker;
import com.shopplatform.domain.shop.service.StoreRoleService;
import com.shopplatform.domain.shop.service.StoreUserService;
import com.shopplatform.framework.security.LoginUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 商户员工与角色管理，对应 docs/prototype/store/staff.html。 */
@RestController
@RequestMapping("/store/staff")
public class StoreStaffController {

    private final StoreUserService storeUserService;
    private final StoreRoleService storeRoleService;
    private final OfflineStoreService offlineStoreService;
    private final PasswordEncoder passwordEncoder;
    private final PackageQuotaChecker packageQuotaChecker;

    public StoreStaffController(StoreUserService storeUserService,
                                 StoreRoleService storeRoleService,
                                 OfflineStoreService offlineStoreService,
                                 PasswordEncoder passwordEncoder,
                                 PackageQuotaChecker packageQuotaChecker) {
        this.storeUserService = storeUserService;
        this.storeRoleService = storeRoleService;
        this.offlineStoreService = offlineStoreService;
        this.passwordEncoder = passwordEncoder;
        this.packageQuotaChecker = packageQuotaChecker;
    }

    @GetMapping("/users")
    public Result<List<StaffItem>> users() {
        Map<Long, StoreRole> roles = storeRoleService.list().stream()
                .collect(Collectors.toMap(StoreRole::getId, Function.identity(), (left, right) -> left));
        List<StaffItem> items = storeUserService.list(Wrappers.<StoreUser>lambdaQuery()
                        .orderByDesc(StoreUser::getIsSuperOwner)
                        .orderByDesc(StoreUser::getCreateTime))
                .stream()
                .map(user -> toItem(user, roles.get(user.getRoleId())))
                .toList();
        return Result.ok(items);
    }

    @GetMapping("/roles")
    public Result<List<RoleItem>> roles() {
        Map<Long, Long> userCounts = storeUserService.list().stream()
                .filter(user -> user.getRoleId() != null)
                .collect(Collectors.groupingBy(StoreUser::getRoleId, Collectors.counting()));
        return Result.ok(storeRoleService.list(Wrappers.<StoreRole>lambdaQuery()
                        .orderByDesc(StoreRole::getIsBuiltin)
                        .orderByAsc(StoreRole::getName))
                .stream()
                .map(role -> new RoleItem(role.getId(), role.getName(), role.getDataScope(), role.getIsBuiltin(), userCounts.getOrDefault(role.getId(), 0L)))
                .toList());
    }

    @PostMapping("/roles")
    public Result<RoleItem> createRole(@Valid @RequestBody SaveRoleRequest request) {
        requireOwner();
        if (storeRoleService.count(Wrappers.<StoreRole>lambdaQuery().eq(StoreRole::getName, request.name().trim())) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "角色名称已存在");
        }
        StoreRole role = new StoreRole();
        role.setName(request.name().trim());
        role.setDataScope(request.dataScope());
        role.setMenuIds("[]");
        role.setIsBuiltin(false);
        storeRoleService.save(role);
        return Result.ok(new RoleItem(role.getId(), role.getName(), role.getDataScope(), false, 0L));
    }

    @PutMapping("/roles/{id}")
    public Result<RoleItem> updateRole(@PathVariable Long id, @Valid @RequestBody SaveRoleRequest request) {
        requireOwner();
        StoreRole role = storeRoleService.getByIdWithTenant(id);
        if (Boolean.TRUE.equals(role.getIsBuiltin())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统内置角色不可编辑");
        }
        long duplicateCount = storeRoleService.count(Wrappers.<StoreRole>lambdaQuery()
                .eq(StoreRole::getName, request.name().trim())
                .ne(StoreRole::getId, id));
        if (duplicateCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "角色名称已存在");
        }
        role.setName(request.name().trim());
        role.setDataScope(request.dataScope());
        storeRoleService.updateById(role);
        long userCount = storeUserService.count(Wrappers.<StoreUser>lambdaQuery().eq(StoreUser::getRoleId, id));
        return Result.ok(new RoleItem(role.getId(), role.getName(), role.getDataScope(), false, userCount));
    }

    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        requireOwner();
        StoreRole role = storeRoleService.getByIdWithTenant(id);
        if (Boolean.TRUE.equals(role.getIsBuiltin())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统内置角色不可删除");
        }
        if (storeUserService.count(Wrappers.<StoreUser>lambdaQuery().eq(StoreUser::getRoleId, id)) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该角色仍有关联员工，不能删除");
        }
        storeRoleService.removeById(role.getId());
        return Result.ok();
    }

    @PostMapping("/users")
    public Result<StaffItem> create(@Valid @RequestBody SaveStaffRequest request) {
        requireOwner();
        packageQuotaChecker.requireStaff();
        if (storeUserService.count(Wrappers.<StoreUser>lambdaQuery().eq(StoreUser::getUsername, request.username())) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "登录账号已存在");
        }
        StoreRole role = storeRoleService.getByIdWithTenant(request.roleId());
        StoreUser user = new StoreUser();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(trimToNull(request.realName()));
        user.setMobile(trimToNull(request.mobile()));
        user.setRoleId(role.getId());
        user.setStoreOfflineId(resolveStoreOfflineId(role, request.storeOfflineId()));
        user.setIsSuperOwner(false);
        user.setStatus(1);
        storeUserService.save(user);
        return Result.ok(toItem(user, role));
    }

    @PutMapping("/users/{id}")
    public Result<StaffItem> update(@PathVariable Long id, @Valid @RequestBody UpdateStaffRequest request) {
        requireOwner();
        StoreUser user = storeUserService.getByIdWithTenant(id);
        if (Boolean.TRUE.equals(user.getIsSuperOwner())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "超级店主账号不可编辑");
        }
        StoreRole role = storeRoleService.getByIdWithTenant(request.roleId());
        user.setRealName(trimToNull(request.realName()));
        user.setMobile(trimToNull(request.mobile()));
        user.setRoleId(role.getId());
        user.setStoreOfflineId(resolveStoreOfflineId(role, request.storeOfflineId()));
        user.setStatus(request.status());
        storeUserService.updateById(user);
        return Result.ok(toItem(user, role));
    }

    @PostMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        requireOwner();
        StoreUser user = storeUserService.getByIdWithTenant(id);
        if (Boolean.TRUE.equals(user.getIsSuperOwner())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "超级店主账号请通过登录页修改密码");
        }
        user.setPassword(passwordEncoder.encode(request.password()));
        storeUserService.updateById(user);
        return Result.ok();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireOwner();
        StoreUser user = storeUserService.getByIdWithTenant(id);
        if (Boolean.TRUE.equals(user.getIsSuperOwner())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "超级店主账号不可删除");
        }
        storeUserService.removeById(user.getId());
        return Result.ok();
    }

    private static StaffItem toItem(StoreUser user, StoreRole role) {
        return new StaffItem(user.getId(), user.getUsername(), user.getRealName(), user.getMobile(),
                user.getRoleId(), role == null ? null : role.getName(), user.getStoreOfflineId(),
                Boolean.TRUE.equals(user.getIsSuperOwner()), user.getStatus(), user.getLastLoginTime(), user.getCreateTime());
    }

    private void requireOwner() {
        LoginUserContext.LoginUser operator = LoginUserContext.get();
        if (operator == null || operator.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录商户后台");
        }
        StoreUser current = storeUserService.getByIdWithTenant(operator.userId());
        if (!Boolean.TRUE.equals(current.getIsSuperOwner())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有超级店主可以管理员工账号");
        }
    }

    /** dataScope=store（门店店员）角色必须指定所属门店；其余角色一律清空，避免残留一个不再生效的门店指向。 */
    private Long resolveStoreOfflineId(StoreRole role, Long requested) {
        if (!"store".equals(role.getDataScope())) {
            return null;
        }
        if (requested == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "门店店员角色必须指定所属门店");
        }
        offlineStoreService.getByIdWithTenant(requested);
        return requested;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record StaffItem(Long id, String username, String realName, String mobile, Long roleId,
                            String roleName, Long storeOfflineId, boolean superOwner, Integer status,
                            java.time.LocalDateTime lastLoginTime, java.time.LocalDateTime createTime) {
    }

    public record RoleItem(Long id, String name, String dataScope, Boolean builtin, long userCount) {
    }

    public record SaveStaffRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 64) String realName,
            @Size(max = 20) String mobile,
            @NotNull Long roleId,
            Long storeOfflineId) {
    }

    public record UpdateStaffRequest(
            @Size(max = 64) String realName,
            @Size(max = 20) String mobile,
            @NotNull Long roleId,
            @NotNull Integer status,
            Long storeOfflineId) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record SaveRoleRequest(@NotBlank @Size(max = 64) String name,
                                  @NotBlank @Size(max = 16) String dataScope) {
    }
}
