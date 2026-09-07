package com.shopplatform.domain.aftersale.service;

import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

/**
 * 售后单服务。见文档三 §3.3、开发计划 Sprint 6。状态机：
 * {@code applying -> approved -> refunding -> refunded}（仅退款，approved后直接进入退款）
 * {@code applying -> approved -> return_shipped -> refunding -> refunded}（退货退款，approved后等买家寄回）
 * {@code applying -> rejected}（拒绝）/ 任意非终态 -> closed（买家撤销）
 */
public interface AfterSaleService extends TenantSafeService<AfterSale> {

    /** 申请售后：校验订单归属、订单已完成收货、该 order_goods 尚无进行中的售后单，按分摊比例计算退款金额。 */
    AfterSale apply(ApplyCommand command);

    /** 商户审核同意：applying -> approved。 */
    void approve(Long afterSaleId, String auditRemark);

    /** 商户审核拒绝：applying -> rejected。 */
    void reject(Long afterSaleId, String auditRemark);

    /** 买家确认已寄回退货（仅退货退款场景需要）：approved -> return_shipped。 */
    void confirmReturnShipped(Long afterSaleId, String expressCompany, String expressNo);

    /**
     * 执行退款：调用微信退款接口，成功后 -> refunded 并按 order_goods.total_num 回补库存（见文档三 §5）。
     * 仅退款场景在 approve 后即可调用；退货退款场景需先 confirmReturnShipped 到 return_shipped。
     */
    void executeRefund(Long afterSaleId);

    /** 买家主动撤销售后申请：非终态 -> closed。 */
    void close(Long afterSaleId);

    List<AfterSale> listByOrderId(Long orderId);

    record ApplyCommand(
            Long orderId,
            Long orderGoodsId,
            Long userId,
            String type,
            String applyReason,
            String applyDesc,
            List<String> images,
            Integer refundNum
    ) {
    }
}
