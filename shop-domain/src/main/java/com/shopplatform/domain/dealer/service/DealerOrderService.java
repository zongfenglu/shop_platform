package com.shopplatform.domain.dealer.service;

import com.shopplatform.domain.dealer.entity.DealerOrder;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface DealerOrderService extends TenantSafeService<DealerOrder> {

    /**
     * 订单支付成功后创建待结算佣金记录。
     * 查找下单用户的上级分销商，按当时佣金比例计算佣金金额。
     */
    void createPending(Long orderId, Long userId, java.math.BigDecimal orderTotal);

    /** 按分销商查询佣金记录。 */
    List<DealerOrder> listByDealer(Long dealerUserId, String status);

    /** Store 端佣金列表。 */
    List<DealerOrder> listByShop(String status);
}
