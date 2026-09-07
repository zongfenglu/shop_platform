package com.shopplatform.domain.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * 秒杀/限时折扣活动商品（一行一 SKU）。见文档三 §3.5、Sprint 9。
 * seckill_num 为秒杀限量（Redis 预扣的快路径池），sold 为 DB 已售（对账任务据此与 Redis 对齐）。
 */
@TableName("seckill_goods")
public class SeckillGoods extends BaseEntity {

    private Long shopId;
    private Long activeId;
    private Long goodsId;
    private Long skuId;
    private BigDecimal seckillPrice;
    private Integer seckillNum;
    /** 每人限购，0=不限 */
    private Integer limitPerUser;
    private Integer sold;
    /** on / off */
    private String status;
    private Integer sort;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getActiveId() { return activeId; }
    public void setActiveId(Long activeId) { this.activeId = activeId; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public BigDecimal getSeckillPrice() { return seckillPrice; }
    public void setSeckillPrice(BigDecimal seckillPrice) { this.seckillPrice = seckillPrice; }
    public Integer getSeckillNum() { return seckillNum; }
    public void setSeckillNum(Integer seckillNum) { this.seckillNum = seckillNum; }
    public Integer getLimitPerUser() { return limitPerUser; }
    public void setLimitPerUser(Integer limitPerUser) { this.limitPerUser = limitPerUser; }
    public Integer getSold() { return sold; }
    public void setSold(Integer sold) { this.sold = sold; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
