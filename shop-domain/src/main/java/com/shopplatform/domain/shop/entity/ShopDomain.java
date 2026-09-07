package com.shopplatform.domain.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 商城域名绑定（泛域名 + 自定义域名）。见文档三 §3.1，本表不带业务 shop_id 隔离语义
 * （查询本表时还不知道 shopId，属于 {@link com.shopplatform.framework.mybatis.ShopTenantLineHandler} 忽略表）。
 */
@TableName("shop_domain")
public class ShopDomain extends BaseEntity {

    private Long shopId;

    private String domain;

    /** sub泛域名 / custom自定义域名 */
    private String type;

    private String certStatus;

    private LocalDateTime certExpireTime;

    private String verifyStatus;

    /** 要求商家把自定义域名 CNAME 到此目标（一般为 {code}.{platformBaseDomain}） */
    private String cnameTarget;

    /** pending/ok/fail/skipped */
    private String cnameStatus;

    private String rejectReason;

    /** 最近一次签发失败原因；成功时清空 */
    private String certError;

    @JsonIgnore
    private String certPemEncrypted;

    @JsonIgnore
    private String keyPemEncrypted;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCertStatus() {
        return certStatus;
    }

    public void setCertStatus(String certStatus) {
        this.certStatus = certStatus;
    }

    public LocalDateTime getCertExpireTime() {
        return certExpireTime;
    }

    public void setCertExpireTime(LocalDateTime certExpireTime) {
        this.certExpireTime = certExpireTime;
    }

    public String getVerifyStatus() {
        return verifyStatus;
    }

    public void setVerifyStatus(String verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    public String getCnameTarget() {
        return cnameTarget;
    }

    public void setCnameTarget(String cnameTarget) {
        this.cnameTarget = cnameTarget;
    }

    public String getCnameStatus() {
        return cnameStatus;
    }

    public void setCnameStatus(String cnameStatus) {
        this.cnameStatus = cnameStatus;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getCertError() {
        return certError;
    }

    public void setCertError(String certError) {
        this.certError = certError;
    }

    public String getCertPemEncrypted() {
        return certPemEncrypted;
    }

    public void setCertPemEncrypted(String certPemEncrypted) {
        this.certPemEncrypted = certPemEncrypted;
    }

    public String getKeyPemEncrypted() {
        return keyPemEncrypted;
    }

    public void setKeyPemEncrypted(String keyPemEncrypted) {
        this.keyPemEncrypted = keyPemEncrypted;
    }
}
