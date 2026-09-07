package com.shopplatform.domain.member.service;

import com.shopplatform.domain.member.entity.UserAddress;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

/**
 * 用户地址簿。见开发计划 Sprint 4「地址管理」。
 * <p>
 * 同购物车：{@code shop_id} 的自动过滤挡不住同商城内的跨用户访问，
 * 所有按 id 的读写都必须显式带 userId 条件。
 */
public interface UserAddressService extends TenantSafeService<UserAddress> {

    /** 该用户的全部地址，默认地址排最前。 */
    List<UserAddress> listByUser(Long userId);

    /** 按 id 查询并校验归属，不属于该用户则抛 404 语义异常。 */
    UserAddress getOwnAddress(Long userId, Long addressId);

    /** 该用户的默认地址；没有默认地址时退化为最近新增的一条；一条都没有则返回 null。 */
    UserAddress findDefault(Long userId);

    /** 新增。首个地址自动置为默认；显式传 isDefault=true 会挤掉原默认地址。 */
    UserAddress create(UserAddress address, Long userId);

    /** 修改。只允许改自己的地址。 */
    void update(UserAddress address, Long userId);

    /** 删除。删掉默认地址后不会自动补选新的默认，由 {@link #findDefault} 的退化逻辑兜底。 */
    void delete(Long userId, Long addressId);

    /** 设为默认地址（同时清掉该用户其它地址的默认标记）。 */
    void setDefault(Long userId, Long addressId);
}
