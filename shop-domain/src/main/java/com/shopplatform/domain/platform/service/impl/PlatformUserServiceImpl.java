package com.shopplatform.domain.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.platform.entity.PlatformRole;
import com.shopplatform.domain.platform.entity.PlatformRoleMenu;
import com.shopplatform.domain.platform.entity.PlatformUser;
import com.shopplatform.domain.platform.entity.PlatformUserRole;
import com.shopplatform.domain.platform.mapper.PlatformMenuMapper;
import com.shopplatform.domain.platform.mapper.PlatformRoleMapper;
import com.shopplatform.domain.platform.mapper.PlatformRoleMenuMapper;
import com.shopplatform.domain.platform.mapper.PlatformUserMapper;
import com.shopplatform.domain.platform.mapper.PlatformUserRoleMapper;
import com.shopplatform.domain.platform.service.PlatformUserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlatformUserServiceImpl extends ServiceImpl<PlatformUserMapper, PlatformUser> implements PlatformUserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PlatformUserRoleMapper userRoleMapper;
    private final PlatformRoleMapper roleMapper;
    private final PlatformRoleMenuMapper roleMenuMapper;
    private final PlatformMenuMapper menuMapper;

    public PlatformUserServiceImpl(PlatformUserRoleMapper userRoleMapper, PlatformRoleMapper roleMapper,
                                    PlatformRoleMenuMapper roleMenuMapper, PlatformMenuMapper menuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public PlatformUser findByUsername(String username) {
        return this.getOne(Wrappers.<PlatformUser>lambdaQuery().eq(PlatformUser::getUsername, username));
    }

    @Override
    @Transactional
    public PlatformUser create(String username, String password, String realName, String mobile, Integer status, Long roleId) {
        PlatformUser existing = findByUsername(username);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "用户名已存在");
        }
        PlatformUser user = new PlatformUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setMobile(mobile);
        user.setStatus(status != null ? status : 1);
        save(user);

        // 分配角色
        if (roleId != null) {
            PlatformUserRole ur = new PlatformUserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        return user;
    }

    @Override
    @Transactional
    public PlatformUser update(Long id, String username, String realName, String mobile, Integer status, Long roleId) {
        PlatformUser user = getByIdWithTenant(id);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在");
        PlatformUser dup = findByUsername(username);
        if (dup != null && !dup.getId().equals(id)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "用户名已被其他管理员使用");
        }
        user.setUsername(username);
        if (realName != null) user.setRealName(realName);
        if (mobile != null) user.setMobile(mobile);
        if (status != null) user.setStatus(status);
        updateById(user);

        // 更新角色
        if (roleId != null) {
            userRoleMapper.delete(Wrappers.<PlatformUserRole>lambdaQuery().eq(PlatformUserRole::getUserId, id));
            PlatformUserRole ur = new PlatformUserRole();
            ur.setUserId(id);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        return user;
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        PlatformUser user = getByIdWithTenant(id);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        PlatformUser user = getByIdWithTenant(id);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在");
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不能禁用超级管理员");
        }
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        updateById(user);
    }

    @Override
    public Long getRoleId(Long userId) {
        PlatformUserRole ur = userRoleMapper.selectOne(
                Wrappers.<PlatformUserRole>lambdaQuery().eq(PlatformUserRole::getUserId, userId));
        return ur != null ? ur.getRoleId() : null;
    }

    @Override
    public List<PlatformRole> listRoles() {
        return roleMapper.selectList(Wrappers.<PlatformRole>lambdaQuery().orderByAsc(PlatformRole::getIsBuiltin));
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                Wrappers.<PlatformRoleMenu>lambdaQuery().eq(PlatformRoleMenu::getRoleId, roleId))
                .stream().map(PlatformRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveRoleMenus(Long roleId, List<Long> menuIds) {
        PlatformRole role = roleMapper.selectById(roleId);
        if (role == null || role.getIsBuiltin() == 1) {
            // 内置角色不允许修改权限（但允许查）
            return;
        }
        roleMenuMapper.delete(Wrappers.<PlatformRoleMenu>lambdaQuery().eq(PlatformRoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                PlatformRoleMenu rm = new PlatformRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
    }

    @Override
    public void touchLastLogin(Long userId) {
        this.update(Wrappers.<PlatformUser>lambdaUpdate()
                .eq(PlatformUser::getId, userId)
                .set(PlatformUser::getLastLoginTime, LocalDateTime.now()));
    }
}
