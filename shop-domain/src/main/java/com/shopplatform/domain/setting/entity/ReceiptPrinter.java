package com.shopplatform.domain.setting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

@TableName("receipt_printer")
public class ReceiptPrinter extends BaseEntity {
    private Long shopId;
    private String name;
    private String provider;
    private String deviceNo;
    private String accessKeyEncrypted;
    private String accessSecretEncrypted;
    private String endpoint;
    private Integer sort;
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getDeviceNo() { return deviceNo; }
    public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }
    public String getAccessKeyEncrypted() { return accessKeyEncrypted; }
    public void setAccessKeyEncrypted(String value) { this.accessKeyEncrypted = value; }
    public String getAccessSecretEncrypted() { return accessSecretEncrypted; }
    public void setAccessSecretEncrypted(String value) { this.accessSecretEncrypted = value; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
