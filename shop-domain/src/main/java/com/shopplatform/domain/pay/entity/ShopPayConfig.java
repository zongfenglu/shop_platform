package com.shopplatform.domain.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/** 商户支付渠道配置。所有渠道私钥与验签公钥都必须加密落库，禁止经 Controller 返回。 */
@TableName("shop_pay_config")
public class ShopPayConfig extends BaseEntity {

    private Long shopId;

    private String channel;

    private String appId;

    private String mchId;

    private String mchCertSerialNo;

    private String apiV3KeyEncrypted;

    private String mchPrivateKeyEncrypted;

    private String alipayPublicKeyEncrypted;

    private String gatewayUrl;

    private Integer sortNo;

    /** enabled/disabled */
    private String status;

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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String mchId) {
        this.mchId = mchId;
    }

    public String getMchCertSerialNo() {
        return mchCertSerialNo;
    }

    public void setMchCertSerialNo(String mchCertSerialNo) {
        this.mchCertSerialNo = mchCertSerialNo;
    }

    public String getApiV3KeyEncrypted() {
        return apiV3KeyEncrypted;
    }

    public void setApiV3KeyEncrypted(String apiV3KeyEncrypted) {
        this.apiV3KeyEncrypted = apiV3KeyEncrypted;
    }

    public String getMchPrivateKeyEncrypted() {
        return mchPrivateKeyEncrypted;
    }

    public void setMchPrivateKeyEncrypted(String mchPrivateKeyEncrypted) {
        this.mchPrivateKeyEncrypted = mchPrivateKeyEncrypted;
    }

    public String getAlipayPublicKeyEncrypted() {
        return alipayPublicKeyEncrypted;
    }

    public void setAlipayPublicKeyEncrypted(String alipayPublicKeyEncrypted) {
        this.alipayPublicKeyEncrypted = alipayPublicKeyEncrypted;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
