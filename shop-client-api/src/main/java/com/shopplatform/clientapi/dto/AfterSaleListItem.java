package com.shopplatform.clientapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 买家售后中心列表项，附带订单与商品快照，避免客户端逐条请求订单详情。 */
public record AfterSaleListItem(
        Long id,
        Long orderId,
        Long orderGoodsId,
        String orderNo,
        String goodsName,
        String goodsImage,
        String specText,
        Integer goodsNum,
        String type,
        String applyReason,
        BigDecimal refundAmount,
        String status,
        LocalDateTime createTime
) {
}
