package com.shopplatform.domain.order.service;

/** 超时自动确认收货。仅处理快递已发货订单，自提走核销。 */
public interface OrderAutoConfirmService {

    int confirmOverdue();
}
