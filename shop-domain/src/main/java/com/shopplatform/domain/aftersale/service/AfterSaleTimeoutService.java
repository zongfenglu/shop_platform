package com.shopplatform.domain.aftersale.service;

/** 售后申请超时自动同意。只改审核状态，不触发微信退款。 */
public interface AfterSaleTimeoutService {

    int approveOverdue();
}
