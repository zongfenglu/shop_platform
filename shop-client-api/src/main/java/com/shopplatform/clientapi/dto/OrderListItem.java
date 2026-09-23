package com.shopplatform.clientapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 消费者订单列表项，附带首件商品快照，避免前端逐单请求详情。 */
public record OrderListItem(
        String id,
        String orderNo,
        BigDecimal payPrice,
        String payStatus,
        String deliveryStatus,
        String orderStatus,
        LocalDateTime createTime,
        String goodsName,
        String goodsImage,
        String specText,
        int goodsCount,
        boolean canApplyAfterSale,
        String afterSaleId,
        String afterSaleStatus,
        String activityType,
        String activityId,
        String groupRecordId,
        String groupStatus,
        Integer groupActualNum,
        Integer groupNum,
        LocalDateTime groupExpireTime
) {
}
