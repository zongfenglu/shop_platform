package com.shopplatform.domain.cart.service;

import com.shopplatform.domain.cart.entity.Cart;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

/**
 * 购物车。见开发计划 Sprint 4「购物车 API」。
 * <p>
 * 归属校验放在本层而不是 Controller：{@code shop_id} 由租户拦截器自动过滤，但同一商城内
 * A 用户拿到 B 用户的 cart id 依然能改——所有按 id 的写操作都必须显式带 userId 条件。
 */
public interface CartService extends TenantSafeService<Cart> {

    /**
     * 加入购物车。同一 SKU 已在车内则累加数量，不新增行。
     * 会校验 SKU 存在、商品在售、库存足够（含车内已有数量）。
     */
    Cart addItem(Long userId, Long skuId, int quantity);

    /** 修改某一行的数量。数量校验同 {@link #addItem}。 */
    void updateQuantity(Long userId, Long cartId, int quantity);

    /** 删除若干行。传入不属于该用户的 id 只会被忽略，不报错也不生效。 */
    void removeItems(Long userId, List<Long> cartIds);

    /** 清空该用户购物车。 */
    void clear(Long userId);

    /** 该用户车内所有行，按加入时间倒序。 */
    List<Cart> listByUser(Long userId);

    /** 车内商品总件数（各行 quantity 之和），用于 tabbar 角标。 */
    int countQuantity(Long userId);
}
