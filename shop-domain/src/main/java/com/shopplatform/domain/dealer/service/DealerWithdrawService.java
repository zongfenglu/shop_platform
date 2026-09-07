package com.shopplatform.domain.dealer.service;

import com.shopplatform.domain.dealer.entity.DealerWithdraw;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface DealerWithdrawService extends TenantSafeService<DealerWithdraw> {

    /** 分销商申请提现。校验可提现金额，冻结对应佣金，创建申请单。 */
    DealerWithdraw apply(Long userId, java.math.BigDecimal amount, String method, String accountInfo);

    /** Store 审批通过（暂不实际打款，标记 approved）。 */
    DealerWithdraw approve(Long id, String remark);

    /** Store 拒绝。解冻佣金。 */
    DealerWithdraw reject(Long id, String remark);

    /** Store 审核列表。 */
    List<DealerWithdraw> listByShop(String status);

    /** 我的提现记录。 */
    List<DealerWithdraw> listByDealer(Long dealerUserId);
}
