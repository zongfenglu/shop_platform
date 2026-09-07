-- ============================================================
-- V6 交易域：运费模板 / 订单 / 订单商品 / 收货地址
-- 见文档三 §3.3；文档二 §2.3 订单
-- user 表（会员域）要到 Sprint 7 才建，本迁移里 order.user_id 先作为普通 BIGINT 占位，
-- 不做外键约束（本项目全程不使用物理外键，靠应用层 shop_id 隔离 + 业务代码保证一致性）。
-- ============================================================

CREATE TABLE freight_template (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    method          VARCHAR(8)      NOT NULL DEFAULT 'count' COMMENT 'weight按重量 / count按件数 / volume按体积',
    rules           JSON            NOT NULL COMMENT '阶梯计费规则，如 [{"region":["*"],"first":1,"firstFee":8,"additional":1,"additionalFee":5}]',
    free_rules      JSON            NULL COMMENT '包邮规则，如 {"minPrice":199,"excludeRegions":["新疆","西藏"]}',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费模板';

CREATE TABLE `order` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    -- 订单号规则：{yyyyMMdd}{shopId后4位}{雪花后8位}，带shop特征便于人工排查，不可顺序可猜，见文档三§3.3
    order_no        VARCHAR(32)     NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '会员域表(user)要到Sprint7才建，此处先占位不做外键',
    seller_id       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '预留B2B2C店铺维度，默认0代表平台/租户自营',

    total_price     DECIMAL(10,2)   NOT NULL COMMENT '商品原价小计',
    discount_price  DECIMAL(10,2)   NOT NULL DEFAULT 0 COMMENT '活动/会员折扣优惠金额',
    coupon_price    DECIMAL(10,2)   NOT NULL DEFAULT 0,
    points_price    DECIMAL(10,2)   NOT NULL DEFAULT 0 COMMENT '积分抵扣金额',
    express_price   DECIMAL(10,2)   NOT NULL DEFAULT 0 COMMENT '运费',
    pay_price       DECIMAL(10,2)   NOT NULL COMMENT '应付金额=total_price-discount_price-coupon_price-points_price+express_price',

    pay_status      VARCHAR(16)     NOT NULL DEFAULT 'unpaid' COMMENT 'unpaid/paid/refunding/refunded',
    pay_method      VARCHAR(16)     NULL COMMENT 'wechat/alipay/balance/cod',
    pay_time        DATETIME        NULL,
    transaction_id  VARCHAR(64)     NULL COMMENT '支付渠道交易号，用于回调幂等对账',

    delivery_type   VARCHAR(16)     NOT NULL DEFAULT 'express' COMMENT 'express快递配送 / pickup门店自提',
    delivery_status VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT 'pending/shipped/received',
    receipt_status  VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT 'pending/confirmed',

    order_status    VARCHAR(16)     NOT NULL DEFAULT 'normal' COMMENT 'normal/cancelled/finished',
    close_reason    VARCHAR(64)     NULL,

    order_source    VARCHAR(16)     NOT NULL DEFAULT 'mp' COMMENT 'mp小程序/h5/app/mp-official公众号',
    remark          VARCHAR(255)    NULL COMMENT '商家备注',
    buyer_remark    VARCHAR(255)    NULL COMMENT '买家留言',

    coupon_id       BIGINT UNSIGNED NULL,
    points_num      INT             NOT NULL DEFAULT 0 COMMENT '本单抵扣的积分数量',
    activity_type   VARCHAR(16)     NOT NULL DEFAULT 'none' COMMENT 'none/seckill/group/bargain',
    activity_id     BIGINT UNSIGNED NULL,

    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,

    UNIQUE KEY uk_order_no (order_no),
    KEY idx_shop_user (shop_id, user_id),
    KEY idx_shop_status (shop_id, order_status, pay_status),
    KEY idx_shop_created (shop_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

CREATE TABLE order_goods (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    goods_id        BIGINT UNSIGNED NOT NULL,
    sku_id          BIGINT UNSIGNED NOT NULL,
    goods_name      VARCHAR(128)    NOT NULL COMMENT '下单时的商品名快照，商品改名不影响历史订单展示',
    image           VARCHAR(255)    NULL,
    spec_text       VARCHAR(128)    NULL COMMENT '规格文案快照，如"杏色/M"',
    goods_price     DECIMAL(10,2)   NOT NULL COMMENT '下单时的SKU单价快照',
    line_price      DECIMAL(10,2)   NULL,
    total_num       INT             NOT NULL,
    total_price     DECIMAL(10,2)   NOT NULL COMMENT '本行商品原价小计 = goods_price * total_num',
    -- 优惠分摊明细：每一笔优惠（活动价/满减/优惠券/积分）按本行实付比例分摊后的金额，
    -- 退款按这里记录的分摊金额算，不重新计算——见文档三§4"优惠分摊是必须做的"
    discount_detail JSON            NULL COMMENT '如 {"coupon":5.20,"points":1.00,"fullReduce":10.00}',
    is_comment      TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已评价',
    refund_status   VARCHAR(16)     NOT NULL DEFAULT 'none' COMMENT 'none/applying/refunded',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_order (shop_id, order_id),
    KEY idx_shop_goods (shop_id, goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品行（含优惠分摊明细）';

CREATE TABLE order_address (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    phone           VARCHAR(20)     NOT NULL,
    province        VARCHAR(32)     NULL,
    city            VARCHAR(32)     NULL,
    region          VARCHAR(32)     NULL,
    detail          VARCHAR(255)    NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_order (shop_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单收货地址快照（下单时复制自用户地址簿，用户改地址簿不影响已下单订单）';
