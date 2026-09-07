package com.shopplatform.domain.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 支付回调幂等日志。见文档三 §9：以 (shop_id, transaction_id) 唯一约束作为并发重复投递的最终防线，
 * 应用层判断只是第一道防线，真正兜底靠这张表的唯一索引。
 */
@TableName("pay_notify_log")
public class PayNotifyLog extends BaseEntity {

    private Long shopId;

    private String channel;

    private String transactionId;

    private String outTradeNo;

    /** success/duplicate/failed */
    private String processResult;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public String getProcessResult() {
        return processResult;
    }

    public void setProcessResult(String processResult) {
        this.processResult = processResult;
    }
}
