package com.shopplatform.domain.setting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

@TableName("store_operation_setting")
public class StoreOperationSetting extends BaseEntity {
    private Long shopId;
    private String uploadProvider;
    private String uploadBucket;
    private String uploadRegion;
    private String uploadEndpoint;
    private String uploadDomain;
    private String uploadAccessKeyIdEncrypted;
    private String uploadAccessKeySecretEncrypted;
    private Integer imageMaxMb;
    private Integer videoMaxMb;
    private Boolean printEnabled;
    private Long printPrinterId;
    private Boolean printOnPaid;
    private Boolean printOnRefund;
    private Integer printCopies;
    private Boolean smsEnabled;
    private String smsNewOrderTemplate;
    private String smsPaidTemplate;
    private String smsShippedTemplate;
    private String smsRefundTemplate;
    private String smsNotifyPhones;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getUploadProvider() { return uploadProvider; }
    public void setUploadProvider(String v) { this.uploadProvider = v; }
    public String getUploadBucket() { return uploadBucket; }
    public void setUploadBucket(String v) { this.uploadBucket = v; }
    public String getUploadRegion() { return uploadRegion; }
    public void setUploadRegion(String v) { this.uploadRegion = v; }
    public String getUploadEndpoint() { return uploadEndpoint; }
    public void setUploadEndpoint(String v) { this.uploadEndpoint = v; }
    public String getUploadDomain() { return uploadDomain; }
    public void setUploadDomain(String v) { this.uploadDomain = v; }
    public String getUploadAccessKeyIdEncrypted() { return uploadAccessKeyIdEncrypted; }
    public void setUploadAccessKeyIdEncrypted(String v) { this.uploadAccessKeyIdEncrypted = v; }
    public String getUploadAccessKeySecretEncrypted() { return uploadAccessKeySecretEncrypted; }
    public void setUploadAccessKeySecretEncrypted(String v) { this.uploadAccessKeySecretEncrypted = v; }
    public Integer getImageMaxMb() { return imageMaxMb; }
    public void setImageMaxMb(Integer v) { this.imageMaxMb = v; }
    public Integer getVideoMaxMb() { return videoMaxMb; }
    public void setVideoMaxMb(Integer v) { this.videoMaxMb = v; }
    public Boolean getPrintEnabled() { return printEnabled; }
    public void setPrintEnabled(Boolean v) { this.printEnabled = v; }
    public Long getPrintPrinterId() { return printPrinterId; }
    public void setPrintPrinterId(Long v) { this.printPrinterId = v; }
    public Boolean getPrintOnPaid() { return printOnPaid; }
    public void setPrintOnPaid(Boolean v) { this.printOnPaid = v; }
    public Boolean getPrintOnRefund() { return printOnRefund; }
    public void setPrintOnRefund(Boolean v) { this.printOnRefund = v; }
    public Integer getPrintCopies() { return printCopies; }
    public void setPrintCopies(Integer v) { this.printCopies = v; }
    public Boolean getSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(Boolean v) { this.smsEnabled = v; }
    public String getSmsNewOrderTemplate() { return smsNewOrderTemplate; }
    public void setSmsNewOrderTemplate(String v) { this.smsNewOrderTemplate = v; }
    public String getSmsPaidTemplate() { return smsPaidTemplate; }
    public void setSmsPaidTemplate(String v) { this.smsPaidTemplate = v; }
    public String getSmsShippedTemplate() { return smsShippedTemplate; }
    public void setSmsShippedTemplate(String v) { this.smsShippedTemplate = v; }
    public String getSmsRefundTemplate() { return smsRefundTemplate; }
    public void setSmsRefundTemplate(String v) { this.smsRefundTemplate = v; }
    public String getSmsNotifyPhones() { return smsNotifyPhones; }
    public void setSmsNotifyPhones(String v) { this.smsNotifyPhones = v; }
}
