package com.shopplatform.domain.dealer.service;

import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface DealerUserService extends TenantSafeService<DealerUser> {

    /** 用户申请成为分销商。status=applying。 */
    DealerUser apply(Long userId);

    /** Store 审批通过。 */
    DealerUser approve(Long id);

    /** Store 拒绝申请。 */
    DealerUser reject(Long id);

    /** Store 禁用分销商。 */
    DealerUser disable(Long id);

    /** Store 端列表（支持状态筛选）。 */
    List<DealerUser> listByShop(String status, String keyword);

    /** 用户端：我的分销资料。 */
    DealerUser getMyDealer(Long userId);

    /** 我的下级分销商列表（一级）。 */
    List<DealerUser> listMyTeam(Long userId);
}
