package com.shopplatform.domain.order.service;

/** 未支付超时关单兜底。主链路是延时消息，本任务扫漏单。 */
public interface OrderTimeoutService {

    int closeOverdue();
}
