package com.shopplatform.domain.mp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

@TableName("mp_authorizer")
public class MpAuthorizer extends BaseEntity {

    private Long shopId;

    /** mini / official */
    private String appType;

    /** self / hosted */
    private String authMode;

    private String appid;

    private String appSecretEncrypted;

    private String refreshTokenEncrypted;

    private String funcInfo;

    /** unauthorized / authorized */
    private String authStatus;

    private String nickName;

    private String onlineVersion;

    private String auditStatus;

    private LocalDateTime authorizedTime;

    private LocalDateTime unauthorizedTime;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getAppSecretEncrypted() {
        return appSecretEncrypted;
    }

    public void setAppSecretEncrypted(String appSecretEncrypted) {
        this.appSecretEncrypted = appSecretEncrypted;
    }

    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public void setRefreshTokenEncrypted(String refreshTokenEncrypted) {
        this.refreshTokenEncrypted = refreshTokenEncrypted;
    }

    public String getFuncInfo() {
        return funcInfo;
    }

    public void setFuncInfo(String funcInfo) {
        this.funcInfo = funcInfo;
    }

    public String getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getOnlineVersion() {
        return onlineVersion;
    }

    public void setOnlineVersion(String onlineVersion) {
        this.onlineVersion = onlineVersion;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    public LocalDateTime getAuthorizedTime() {
        return authorizedTime;
    }

    public void setAuthorizedTime(LocalDateTime authorizedTime) {
        this.authorizedTime = authorizedTime;
    }

    public LocalDateTime getUnauthorizedTime() {
        return unauthorizedTime;
    }

    public void setUnauthorizedTime(LocalDateTime unauthorizedTime) {
        this.unauthorizedTime = unauthorizedTime;
    }
}
