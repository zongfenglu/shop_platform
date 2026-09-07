-- Sprint 8 优惠券 + 满减。见文档三 §3.5 营销域。
-- 优惠券与满减规则都是租户级（带 shop_id），由 TenantLineInnerInterceptor 自动隔离。

-- 1) 优惠券
CREATE TABLE `coupon` (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    name                VARCHAR(64)     NOT NULL,
    type                VARCHAR(10)     NOT NULL COMMENT 'reduce满减券 / discount折扣券',
    reduce_price        DECIMAL(10,2)   NULL COMMENT 'type=reduce 时的满减金额',
    discount_ratio      DECIMAL(3,2)   NULL COMMENT 'type=discount 时的折扣 0.90=九折',
    min_price           DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '使用门槛（满X可用）',
    expire_type         VARCHAR(10)     NOT NULL COMMENT 'fixed固定时间段 / receive领取后N天',
    start_time          DATETIME        NULL COMMENT 'expire_type=fixed 的开始时间',
    end_time            DATETIME        NULL COMMENT 'expire_type=fixed 的结束时间',
    expire_days        INT             NULL COMMENT 'expire_type=receive 领取后有效天数',
    total_num           INT             NOT NULL DEFAULT 0 COMMENT '发放总量，0=不限',
    received_num        INT             NOT NULL DEFAULT 0 COMMENT '已领取数',
    limit_per_user      INT             NOT NULL DEFAULT 1 COMMENT '每人限领',
    apply_range         VARCHAR(10)     NOT NULL DEFAULT 'all' COMMENT 'all/category/goods',
    apply_range_config  JSON            NULL COMMENT 'apply_range=category 时存分类id数组，=goods 时存商品id数组',
    status              VARCHAR(10)     NOT NULL DEFAULT 'on' COMMENT 'on/off/ended',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';

-- 2) 用户领取的券。snapshot 存领取时的券关键信息，券后续改动不影响已领取的券。
CREATE TABLE `user_coupon` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    coupon_id       BIGINT UNSIGNED NOT NULL,
    snapshot        JSON            NOT NULL COMMENT '领取时的券快照（name/type/reduce_price/discount_ratio/min_price/apply_range/apply_range_config）',
    status          VARCHAR(10)     NOT NULL DEFAULT 'unused' COMMENT 'unused/used/expired',
    start_time      DATETIME        NULL,
    end_time        DATETIME        NULL COMMENT '失效时间，过期任务据此置 expired',
    use_order_id    BIGINT UNSIGNED NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_user (shop_id, user_id),
    KEY idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户领取的优惠券';

-- 3) 满减/满件折规则。rules 为 JSON 数组：
--    type=money: [{"threshold":100.00,"reduce":10.00}, ...]  取满足门槛中最大的一档
--    type=count: [{"threshold":2,"discount":0.90}, ...]        满N件打M折
CREATE TABLE `full_reduce_rule` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    type            VARCHAR(10)     NOT NULL COMMENT 'money按金额满减 / count按件数折',
    rules           JSON            NOT NULL,
    free_express    TINYINT         NOT NULL DEFAULT 0 COMMENT '是否包邮',
    status          VARCHAR(10)     NOT NULL DEFAULT 'on' COMMENT 'on/off',
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减/满件折规则';
