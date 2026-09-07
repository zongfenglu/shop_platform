package com.shopplatform.domain.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 商户支付配置（租户自有微信商户号）。见文档三 §9：
 * {@code apiV3KeyEncrypted}/{@code mchPrivateKeyEncrypted} 落库前必须经 AesGcmEncryptor 加密，
 * 本类只保存密文字段名——任何读出这两个字段直接展示给前端的代码都是 bug。
 */
@TableName("shop_pay_config")
public class ShopPayConfig extends BaseEntity {

    private Long shopId;

    private String channel;

    private String appId;

    private String mchId;

    private String mchCertSerialNo;

    private String apiV3KeyEncrypted;

    private String mchPrivateKeyEncrypted;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
