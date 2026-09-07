package com.shopplatform.framework.tenant;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.function.Supplier;

/**
 * 租户上下文：贯穿全链路的 shop_id 载体。
 * <p>
 * 使用 {@link TransmittableThreadLocal} 而非 {@code InheritableThreadLocal}，
 * 因为线程池复用场景下 InheritableThreadLocal 只在线程创建时拷贝一次父线程的值，
 * 之后线程被复用执行别的租户的任务时值不会更新，会导致跨租户串号——
 * 这是多租户系统最经典的生产事故之一（异步任务、@Async、MQ 消费、定时任务分片场景尤其容易踩中）。
 * <p>
 * 见文档三 §2.1。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> SHOP_ID = new TransmittableThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE = new TransmittableThreadLocal<>();

    private TenantContext() {
    }

    public static Long get() {
        return SHOP_ID.get();
    }

    /**
     * 获取当前租户 ID，若未设置则抛出异常。
     * 用于业务代码中"必须有租户上下文"的场景，避免 NPE 被悄悄掩盖。
     */
    public static Long getRequired() {
        Long shopId = SHOP_ID.get();
        if (shopId == null) {
            throw new IllegalStateException("当前线程缺少租户上下文（shopId），请检查是否经过租户识别过滤器或未在异步任务中透传");
        }
        return shopId;
    }

    public static void set(Long shopId) {
        SHOP_ID.set(shopId);
    }

    public static boolean isIgnoreTenant() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    public static void clear() {
        SHOP_ID.remove();
        IGNORE.remove();
    }

    /**
     * 仅平台超管的跨租户查询可用，必须显式声明。
     * 用于超管后台统计报表等需要一次性跨所有租户查询的场景。
     * 执行完毕后自动恢复原有的忽略标记，不污染外层上下文。
     */
    public static <T> T ignoreTenant(Supplier<T> fn) {
        Boolean previous = IGNORE.get();
        IGNORE.set(Boolean.TRUE);
        try {
            return fn.get();
        } finally {
            if (previous == null) {
                IGNORE.remove();
            } else {
                IGNORE.set(previous);
            }
        }
    }

    public static void ignoreTenant(Runnable fn) {
        ignoreTenant(() -> {
            fn.run();
            return null;
        });
    }
}
