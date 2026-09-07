package com.shopplatform.domain.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城会员（消费者）。见 V7__member_domain.sql 注释：本表是 Sprint 7 完整会员体系的最小先行版本，
 * 等级/积分/余额字段由 V12 直接在这张表上 ALTER TABLE 补齐，不要另建新表。
 * <p>
 * 多租户语义（文档三 §3.4）：user 表以 (shop_id, open_id) 唯一，同一个微信用户在不同租户是两个独立账号，
 * 余额/积分互不相通——这是"多开"的核心语义，不能用全局用户表。
 */
@TableName("`user`")
public class Member extends BaseEntity {

    private Long shopId;

    private String mobile;

    private String nickname;

    private String avatar;

    /** 1正常 0禁用 */
    private Integer status;

    /** 性别 0未知 1男 2女 */
    private Integer gender;

    /** 来源端 mp/h5/app/mp-official */
    private String platform;

    private String openId;

    private String unionId;

    /** 可用余额 */
    private BigDecimal balance;

    /** 可用积分 */
    private Integer points;

    /** 成长值，等级升级依据 */
    private Integer growthValue;

    /** 当前会员等级 user_grade.id */
    private Long gradeId;

    /** 累计实付金额 */
    private BigDecimal payMoney;

    /** 累计成交订单数 */
    private Integer payCount;

    private LocalDateTime lastLoginTime;

    /** 是否拉黑 0否 1是 */
    private Integer isBlack;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getGrowthValue() {
        return growthValue;
    }

    public void setGrowthValue(Integer growthValue) {
        this.growthValue = growthValue;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
    }

    public BigDecimal getPayMoney() {
        return payMoney;
    }

    public void setPayMoney(BigDecimal payMoney) {
        this.payMoney = payMoney;
    }

    public Integer getPayCount() {
        return payCount;
    }

    public void setPayCount(Integer payCount) {
        this.payCount = payCount;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Integer getIsBlack() {
        return isBlack;
    }

    public void setIsBlack(Integer isBlack) {
        this.isBlack = isBlack;
    }
}
