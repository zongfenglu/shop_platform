package com.shopplatform.common.result;

/**
 * 错误码分段（文档三 §8）：
 * 1xxxx 通用 / 2xxxx 租户与套餐 / 3xxxx 商品 / 4xxxx 订单 / 5xxxx 支付 / 6xxxx 营销 / 7xxxx 装修
 */
public enum ErrorCode {

    // ---- 1xxxx 通用 ----
    PARAM_INVALID(10001, "参数错误"),
    UNAUTHORIZED(10002, "未登录或登录已过期"),
    FORBIDDEN(10003, "无权限访问"),
    NOT_FOUND(10004, "资源不存在"),
    SYSTEM_ERROR(10500, "系统繁忙，请稍后重试"),

    // ---- 2xxxx 租户与套餐 ----
    TENANT_NOT_FOUND(20000, "商城不存在"),
    TENANT_EXPIRED(20001, "商城套餐已过期"),
    TENANT_DISABLED(20002, "商城已被停用"),
    QUOTA_EXCEEDED(20003, "配额已达上限"),
    PACKAGE_FEATURE_LOCKED(20004, "当前套餐不支持该功能"),
    SHOP_ORDER_PENDING_EXISTS(20020, "已有待确认的套餐订单，请等待平台处理"),
    SHOP_ORDER_STATUS_INVALID(20021, "订购单状态不允许该操作"),
    INVOICE_ORDER_NOT_PAID(20022, "订购单未支付，不能申请发票"),
    INVOICE_ALREADY_EXISTS(20023, "该订购单已有发票申请"),
    INVOICE_STATUS_INVALID(20024, "发票状态不允许该操作"),
    IMPERSONATE_TICKET_INVALID(20005, "免密登录已失效，请重新发起"),
    DOMAIN_INVALID(20010, "域名格式不正确"),
    DOMAIN_ALREADY_BOUND(20011, "该域名已被占用或本店已有待审核/已生效的自定义域名"),
    DOMAIN_CNAME_MISMATCH(20012, "CNAME 尚未正确解析到平台域名"),
    DOMAIN_STATUS_INVALID(20013, "当前域名状态不允许该操作"),
    ACME_NOT_ENABLED(20040, "Let's Encrypt 未启用"),
    ACME_ISSUE_FAILED(20041, "证书签发失败"),
    WECHAT_COMPONENT_NOT_CONFIGURED(20030, "尚未配置微信开放平台第三方"),
    WECHAT_TICKET_MISSING(20031, "尚未收到 component_verify_ticket"),
    WECHAT_OPEN_API_ERROR(20032, "微信开放平台接口调用失败"),
    MP_AUTHORIZER_EXISTS(20033, "该类型客户端已绑定"),
    MP_AUTHORIZER_NOT_FOUND(20034, "未找到小程序或公众号授权"),

    // ---- 3xxxx 商品 ----
    GOODS_NOT_FOUND(30000, "商品不存在"),
    GOODS_OFF_SHELF(30001, "商品已下架"),
    SKU_STOCK_INSUFFICIENT(30002, "库存不足"),

    // ---- 4xxxx 订单 ----
    ORDER_NOT_FOUND(40000, "订单不存在"),
    ORDER_STATUS_INVALID(40001, "订单状态不允许该操作"),
    OFFLINE_STORE_DISABLED(40010, "门店暂不支持自提"),
    VERIFY_CODE_INVALID(40011, "核销码无效"),

    // ---- 5xxxx 支付 ----
    PAY_CHANNEL_ERROR(50000, "支付渠道异常"),
    PAY_CALLBACK_INVALID_SIGN(50001, "支付回调签名校验失败"),

    // ---- 6xxxx 营销 ----
    COUPON_NOT_AVAILABLE(60000, "优惠券不可用"),
    ACTIVITY_EXPIRED(60001, "活动已结束"),
    SECKILL_LIMIT_EXCEEDED(60002, "超出秒杀限购数量"),
    SECKILL_GOODS_NOT_FOUND(60003, "秒杀商品不存在"),
    GROUP_NOT_FOUND(60010, "拼团活动不存在"),
    GROUP_FULL(60011, "拼团已满或已结束"),
    GROUP_EXPIRED(60012, "拼团已过期"),
    BARGAIN_NOT_FOUND(60020, "砍价活动不存在"),
    BARGAIN_EXPIRED(60021, "砍价已过期"),
    BARGAIN_FLOOR_REACHED(60022, "砍价已到底价"),
    BARGAIN_HELP_LIMIT(60023, "砍价助力次数已达上限"),
    POINTS_INSUFFICIENT(60030, "积分不足"),
    SIGN_ALREADY_SIGNED(60031, "今日已签到"),
    SIGN_MAKEUP_INVALID(60032, "补签日期无效"),
    POINTS_GOODS_NOT_FOUND(60033, "兑换商品不存在或已下架"),
    EXCHANGE_STATUS_INVALID(60034, "兑换单状态不允许该操作"),
    EXCHANGE_STOCK_INSUFFICIENT(60035, "兑换商品库存不足"),
    DEALER_DISABLED(60040, "分销功能未开启"),
    DEALER_ALREADY_APPLIED(60041, "已有分销申请或已是分销商"),
    DEALER_NOT_FOUND(60042, "分销商不存在"),
    DEALER_SETTING_NOT_CONFIGURED(60043, "分销设置未配置"),

    // ---- 7xxxx 装修（DIY） ----
    DIY_PAGE_DATA_INVALID(70000, "页面内容格式不合法"),
    DIY_PAGE_TYPE_NOT_SUPPORTED(70001, "该类型页面不支持此操作"),
    DIY_TABBAR_ITEM_COUNT_INVALID(70002, "底部导航菜单项需在2~5个之间"),
    UPLOAD_FILE_EMPTY(70003, "上传文件为空"),
    UPLOAD_FILE_TOO_LARGE(70004, "图片大小不能超过5MB"),
    UPLOAD_FILE_TYPE_NOT_ALLOWED(70005, "仅支持 jpg/png/gif/webp 格式的图片");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
