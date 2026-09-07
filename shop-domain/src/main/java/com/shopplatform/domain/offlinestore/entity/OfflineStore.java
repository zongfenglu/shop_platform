package com.shopplatform.domain.offlinestore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/** 门店自提点。Sprint 13：offline_store/store_clerk/verify_log。 */
@TableName("offline_store")
public class OfflineStore extends BaseEntity {

    private Long shopId;
    private String name;
    private String logo;
    private String phone;
    private String region;
    private String detail;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String businessHours;
    /** enabled/disabled */
    private String status;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
