package com.shopplatform.domain.shop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 商城（租户）主体。见文档三 §3.1，本表不带 shop_id（它本身就是租户的根），
 * 属于 {@link com.shopplatform.framework.mybatis.ShopTenantLineHandler} 的忽略表白名单。
 */
@TableName("shop")
public class Shop extends BaseEntity {

    /** 二级域名前缀，如 huajianji，全局唯一 */
    private String code;

    private String name;

    private String logo;

    /** 服饰鞋包/生鲜食品/美妆个护/3C数码/其他 */
    private String industry;

    private String contact;

    private String mobile;

    /** trial/normal/expired/disabled/archived，对应 {@link com.shopplatform.common.enums.ShopStatus} */
    private String status;

    private LocalDateTime expireTime;

    /** 当前生效的 shop_package.id */
    private Long packageId;

    @TableField(exist = false)
    private String packageName;

    @TableField(condition = "remark IS NOT NULL")
    private String remark;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
