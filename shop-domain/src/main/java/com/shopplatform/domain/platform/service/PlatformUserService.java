package com.shopplatform.domain.platform.service;

import com.shopplatform.domain.platform.entity.PlatformRole;
import com.shopplatform.domain.platform.entity.PlatformUser;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface PlatformUserService extends TenantSafeService<PlatformUser> {

    PlatformUser findByUsername(String username);

    /** 新建平台管理员，同时分配角色。 */
    PlatformUser create(String username, String password, String realName, String mobile, Integer status, Long roleId);

    /** 编辑平台管理员，同时更新角色。 */
    PlatformUser update(Long id, String username, String realName, String mobile, Integer status, Long roleId);

    /** 重置密码。 */
    void resetPassword(Long id, String newPassword);

    /** 切换启用/禁用。 */
    void toggleStatus(Long id);

    /** 获取用户所属角色ID（取第一个）。 */
    Long getRoleId(Long userId);

    /** 所有角色列表。 */
    List<PlatformRole> listRoles();

    /** 获取某个角色的菜单权限ID列表。 */
    List<Long> getRoleMenuIds(Long roleId);

    /** 保存角色的菜单权限（先删后插）。 */
    void saveRoleMenus(Long roleId, List<Long> menuIds);

    void touchLastLogin(Long userId);
}
