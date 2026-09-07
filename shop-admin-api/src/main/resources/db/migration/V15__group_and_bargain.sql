-- Sprint 10 拼团 + 砍价。见文档三 §3.5 营销域、§4 价格引擎（ActivityPriceHandler 互斥取活动价）。
-- 与秒杀/限时折扣共用 ActivityPriceHandler：activityType=group/bargain 时取活动价替换原价。
-- 拼团：发起人开团 → 拼团记录 pending → 参团人数达 group_num 成团 → 超时未成团原路退款（GroupExpireJob）。
-- 砍价：发起人开砍 → 好友助力逐刀降价至 floor_price 或 help_limit → 以 current_price 下单。

-- 1) 拼团活动
CREATE TABLE `group_active` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    goods_id        BIGINT UNSIGNED NOT NULL,
    group_num       INT             NOT NULL COMMENT '成团人数（含团长）',
    group_price     JSON            NOT NULL COMMENT '{"skuId": price} 拼团价（按 SKU）',
    valid_hours     INT             NOT NULL COMMENT '开团后有效时长（小时），超时未成团自动退款',
    is_mock         TINYINT         NOT NULL DEFAULT 0 COMMENT '0否 1是：人数不足时模拟成团（仅演示）',
    start_time      DATETIME        NOT NULL COMMENT '活动开始时间',
    end_time        DATETIME        NOT NULL COMMENT '活动结束时间',
    status          VARCHAR(8)      NOT NULL DEFAULT 'on' COMMENT 'on/off',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id),
    KEY idx_goods (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动';

-- 2) 拼团记录（一个团一条）
CREATE TABLE `group_record` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    active_id       BIGINT UNSIGNED NOT NULL,
    leader_user_id  BIGINT UNSIGNED NOT NULL COMMENT '团长用户 id',
    leader_order_id BIGINT UNSIGNED NULL COMMENT '团长订单 id',
    status          VARCHAR(8)     NOT NULL DEFAULT 'pending' COMMENT 'pending/success/fail',
    actual_num      INT             NOT NULL DEFAULT 1 COMMENT '当前参团人数（含团长）',
    expire_time     DATETIME        NOT NULL COMMENT '成团截止时间（开团时间+valid_hours）',
    success_time    DATETIME        NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id),
    KEY idx_active (active_id),
    KEY idx_status_expire (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团记录';

-- 3) 砍价活动
CREATE TABLE `bargain_active` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    goods_id        BIGINT UNSIGNED NOT NULL,
    floor_price     DECIMAL(10,2)   NOT NULL COMMENT '底价（砍到不能再砍）',
    valid_hours     INT             NOT NULL COMMENT '发起后有效时长（小时）',
    help_limit      INT             NOT NULL DEFAULT 0 COMMENT '助力次数上限，0=不限',
    start_time      DATETIME        NOT NULL,
    end_time        DATETIME        NOT NULL,
    status          VARCHAR(8)      NOT NULL DEFAULT 'on' COMMENT 'on/off',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id),
    KEY idx_goods (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='砍价活动';

-- 4) 砍价记录（一个用户对一个活动一条）
CREATE TABLE `bargain_record` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    active_id       BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    current_price   DECIMAL(10,2)   NOT NULL COMMENT '当前已砍到的价格',
    help_count      INT             NOT NULL DEFAULT 0 COMMENT '已助力次数',
    status          VARCHAR(8)     NOT NULL DEFAULT 'ongoing' COMMENT 'ongoing/done/expired/ordered',
    expire_time     DATETIME        NOT NULL COMMENT '砍价截止时间（发起+valid_hours）',
    order_id        BIGINT UNSIGNED NULL COMMENT '砍价成功后下单的订单 id',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_active_user (shop_id, active_id, user_id),
    KEY idx_status_expire (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='砍价记录';
