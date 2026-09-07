package com.shopplatform.domain.ssl.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

@TableName("acme_account")
public class AcmeAccount extends BaseEntity {

    private String directoryUrl;

    private String accountUrl;

    private String contactEmail;

    private String keyPemEncrypted;

    public String getDirectoryUrl() {
        return directoryUrl;
    }

    public void setDirectoryUrl(String directoryUrl) {
        this.directoryUrl = directoryUrl;
    }

    public String getAccountUrl() {
        return accountUrl;
    }

    public void setAccountUrl(String accountUrl) {
        this.accountUrl = accountUrl;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getKeyPemEncrypted() {
        return keyPemEncrypted;
    }

    public void setKeyPemEncrypted(String keyPemEncrypted) {
        this.keyPemEncrypted = keyPemEncrypted;
    }
}
