package com.shopplatform.clientapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 购物车结算请求，预览({@code /api/checkout/preview})和提交({@code /api/checkout/submit})共用同一结构，
 * 避免两处各拼一套字段导致"预览和提交传的东西对不上"。
 */
public record CheckoutRequest(
        @NotEmpty @Valid List<CartItemRequest> items,
        @NotNull String deliveryType,
        /** deliveryType=pickup 时必填：自提门店 id（offline_store.id） */
        Long pickupStoreId,
        Long freightTemplateId,
        Long couponId,
        Integer pointsToUse,
        String activityType,
        Long activityId,
        /** 拼团参团时传要加入的 group_record.id；开团/非拼团传 null */
        Long groupRecordId,
        String buyerRemark,
        @Valid AddressRequest address,
        /**
         * 本单来自购物车的哪几行。下单成功后由服务端在同一事务里删除，
         * 让"下单"和"清购物车"要么都成功要么都不发生——放在前端下单后再调一次删除接口的话，
         * 那次调用失败（断网/切后台）就会留下已下单却还在车里的幽灵行。
         * 立即购买场景不传。
         */
        List<Long> cartIds
) {
}
