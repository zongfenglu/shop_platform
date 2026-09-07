package com.shopplatform.domain.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 商户员工账号。见文档三 §3.6、文档二 §2.12。
 * (shop_id, username) 唯一——不同商城可以有同名账号，互不冲突。
 */
@TableName("store_user")
public class StoreUser extends BaseEntity {

    private Long shopId;

    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    private String realName;

    private String mobile;

    private Long roleId;

    /** 门店店员角色时关联的门店ID，见 offline_store 表（M3阶段建表） */
    private Long storeOfflineId;

    /** 是否为超级店主（建店时自动创建，不可删除） */
    private Boolean isSuperOwner;

    private Integer status;

    private LocalDateTime lastLoginTime;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getStoreOfflineId() {
        return storeOfflineId;
    }

    public void setStoreOfflineId(Long storeOfflineId) {
        this.storeOfflineId = storeOfflineId;
    }

    public Boolean getIsSuperOwner() {
        return isSuperOwner;
    }

    public void setIsSuperOwner(Boolean isSuperOwner) {
        this.isSuperOwner = isSuperOwner;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}
