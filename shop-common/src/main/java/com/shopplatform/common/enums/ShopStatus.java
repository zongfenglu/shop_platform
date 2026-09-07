package com.shopplatform.common.enums;

/**
 * 商城（租户）生命周期状态。见文档一 §3.3 租户生命周期。
 */
public enum ShopStatus {

    /** 试用中 */
    TRIAL("trial", "试用中"),
    /** 正常运营 */
    NORMAL("normal", "正常"),
    /** 已过期：仅可查看数据、发货、处理售后、续费；禁止上新/改价/开活动/装修 */
    EXPIRED("expired", "已过期"),
    /** 已停用：不可登录，用户端不可访问 */
    DISABLED("disabled", "已停用"),
    /** 已归档：数据只读，等待清理 */
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String desc;

    ShopStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 该状态下是否允许用户端下单交易 */
    public boolean canTrade() {
        return this == TRIAL || this == NORMAL;
    }

    /** 该状态下是否允许登录商户后台 */
    public boolean canLogin() {
        return this != DISABLED && this != ARCHIVED;
    }
}
