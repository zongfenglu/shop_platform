package com.shopplatform.domain.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 套餐模板（平台侧配置）。见文档三 §3.1、文档一 §3.4。
 * 修改本表不影响已开通商城——已开通的商城读取的是 {@link ShopPackage} 的快照。
 */
@TableName("package_tpl")
public class PackageTpl extends BaseEntity {

    private String name;

    private String intro;

    /** JSON：功能菜单集合，如 ["goods.*","marketing.coupon","marketing.seckill"] */
    private String menus;

    /** JSON：配额上限，如 {"goods_max":5000,"staff_max":10,"storage_mb":20480,"sms_month":1000} */
    private String quota;

    /** JSON：价格，如 {"month":299,"quarter":799,"year":2680} */
    private String price;

    private Boolean isTrial;

    private Boolean isShow;

    private Integer sort;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Boolean getIsTrial() {
        return isTrial;
    }

    public void setIsTrial(Boolean isTrial) {
        this.isTrial = isTrial;
    }

    public Boolean getIsShow() {
        return isShow;
    }

    public void setIsShow(Boolean isShow) {
        this.isShow = isShow;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
