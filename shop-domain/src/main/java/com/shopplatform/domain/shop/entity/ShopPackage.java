package com.shopplatform.domain.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 商城套餐开通记录（快照机制）。见文档一 §3.4：
 * 套餐字段做快照——商城开通时把套餐内容快照到本表，之后平台改套餐模板不影响已开通商城
 * （除非平台显式"同步"）。这是 SaaS 计费系统最容易踩的坑，不能让本表的数据随 package_tpl 变化而漂移。
 */
@TableName("shop_package")
public class ShopPackage extends BaseEntity {

    private Long shopId;

    /** 来源套餐模板ID，仅用于追溯，不做外键约束（模板可能已变化或被删除） */
    private Long packageTplId;

    private String name;

    private String menus;

    private String quota;

    /** 价格快照，用于订单追溯，不是实时价格 */
    private String priceSnapshot;

    private LocalDateTime startTime;

    private LocalDateTime expireTime;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getPackageTplId() {
        return packageTplId;
    }

    public void setPackageTplId(Long packageTplId) {
        this.packageTplId = packageTplId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMenus() {
        return menus;
    }

    public void setMenus(String menus) {
        this.menus = menus;
    }

    public String getQuota() {
        return quota;
    }

    public void setQuota(String quota) {
        this.quota = quota;
    }

    public String getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(String priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
