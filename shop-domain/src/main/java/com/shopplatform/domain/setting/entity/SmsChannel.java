package com.shopplatform.domain.setting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

@TableName("sms_channel")
public class SmsChannel extends BaseEntity {
    private Long shopId;
    private String name;
    private String provider;
    private String appId;
    private String accessKeyIdEncrypted;
    private String accessKeySecretEncrypted;
    private String signName;
    private String endpoint;
    private Integer priority;
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAccessKeyIdEncrypted() { return accessKeyIdEncrypted; }
    public void setAccessKeyIdEncrypted(String value) { this.accessKeyIdEncrypted = value; }
    public String getAccessKeySecretEncrypted() { return accessKeySecretEncrypted; }
    public void setAccessKeySecretEncrypted(String value) { this.accessKeySecretEncrypted = value; }
    public String getSignName() { return signName; }
    public void setSignName(String signName) { this.signName = signName; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
