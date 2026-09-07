package com.shopplatform.framework.security;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 当前登录用户上下文（商户员工 / 消费者 / 平台管理员均可用），
 * 与 {@link com.shopplatform.framework.tenant.TenantContext} 分开维护：
 * TenantContext 只关心"这次请求属于哪个租户"，LoginUserContext 关心"是谁在操作"，
 * 两者职责不同但生命周期一致，均由各端的鉴权 Filter 在请求结束时清理。
 */
public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new TransmittableThreadLocal<>();

    private LoginUserContext() {
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 是否为平台代管理操作（免密登录进入商户后台），对应文档二 §1.2 的 by_platform 审计标记。 */
    public static boolean isPlatformImpersonation() {
        LoginUser user = HOLDER.get();
        return user != null && user.platformImpersonation();
    }

    public record LoginUser(Long userId, Long shopId, String username, boolean platformImpersonation) {
    }
}
