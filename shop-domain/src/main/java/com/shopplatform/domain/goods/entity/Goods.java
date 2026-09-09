package com.shopplatform.domain.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

/**
 * 商品主表。见文档三 §3.2、文档二 §2.2。
 */
@TableName("goods")
public class Goods extends BaseEntity {

    private Long shopId;

    /** JSON数组：所属分类ID集合 */
    private String categoryIds;

    private Long brandId;

    private String name;

    private String subName;

    private String code;

    /** JSON数组：商品主图，第一张为封面 */
    private String images;

    private String video;

    /** single单规格 / multi多规格 */
    private String specType;

    private String content;

    /** on出售中 / off仓库中 / deleted回收站 */
    private String status;

    private Integer salesInitial;

    private Integer salesActual;

    /** 库存汇总（各SKU库存之和，冗余字段便于列表展示排序） */
    private Integer stockTotal;

    /** JSON数组：配送方式，如 ["express"] 或 ["express","pickup"] */
    private String deliveryType;

    private Long freightTemplateId;

    private java.math.BigDecimal freightFee;

    /** JSON数组：关联的服务保障ID集合 */
    private String serviceIds;

    private Boolean isVirtual;

    /** none不限购 / single单次限购 */
    private String limitType;

    private Integer limitNum;

    /** 单品分销佣金比例（百分比）。null=使用店铺 dealer_setting 的默认比例。仅 commission_type=goods 时生效 */
    private java.math.BigDecimal commissionRate;

    private Integer sort;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(String categoryIds) {
        this.categoryIds = categoryIds;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public String getSpecType() {
        return specType;
    }

    public void setSpecType(String specType) {
        this.specType = specType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSalesInitial() {
        return salesInitial;
    }

    public void setSalesInitial(Integer salesInitial) {
        this.salesInitial = salesInitial;
    }

    public Integer getSalesActual() {
        return salesActual;
    }

    public void setSalesActual(Integer salesActual) {
        this.salesActual = salesActual;
    }

    public Integer getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(Integer stockTotal) {
        this.stockTotal = stockTotal;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public Long getFreightTemplateId() {
        return freightTemplateId;
    }

    public void setFreightTemplateId(Long freightTemplateId) {
        this.freightTemplateId = freightTemplateId;
    }

    public java.math.BigDecimal getFreightFee() {
        return freightFee;
    }

    public void setFreightFee(java.math.BigDecimal freightFee) {
        this.freightFee = freightFee;
    }

    public String getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(String serviceIds) {
        this.serviceIds = serviceIds;
    }

    public Boolean getIsVirtual() {
        return isVirtual;
    }

    public void setIsVirtual(Boolean isVirtual) {
        this.isVirtual = isVirtual;
    }

    public String getLimitType() {
        return limitType;
    }

    public void setLimitType(String limitType) {
        this.limitType = limitType;
    }

    public Integer getLimitNum() {
        return limitNum;
    }

    public void setLimitNum(Integer limitNum) {
        this.limitNum = limitNum;
    }

    public java.math.BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(java.math.BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
