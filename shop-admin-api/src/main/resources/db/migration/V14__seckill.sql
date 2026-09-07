-- Sprint 9 秒杀 + 限时折扣。见文档三 §3.5 营销域、§4 价格引擎、§5 库存与并发。
-- 三张表均带 shop_id，由 TenantLineInnerInterceptor 自动隔离。
-- 限时折扣与秒杀共用 seckill_active：time_ids 为空 = 限时折扣（全天仅换价、无限购）；
--   time_ids 非空 = 秒杀（场次制 + 限购 + Redis 预扣）。seckill_goods 一行一 SKU，便于按 SKU 预扣/限购/对账。

-- 1) 秒杀场次（每日固定时段，如 10:00-12:00）
CREATE TABLE `seckill_time` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(32)     NOT NULL COMMENT '场次名称，如 10:00场',
    start_time      TIME            NOT NULL COMMENT '开始时分 HH:mm:ss',
    end_time        TIME            NOT NULL COMMENT '结束时分',
    sort            INT             NOT NULL DEFAULT 0,
    status          VARCHAR(8)      NOT NULL DEFAULT 'on' COMMENT 'on/off',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀场次（每日固定时段）';

-- 2) 秒杀/限时折扣活动。time_ids 为 JSON 数组（seckill_time.id），空数组=限时折扣全天有效。
CREATE TABLE `seckill_active` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    time_ids        JSON            NULL COMMENT '场次id数组；空/null=限时折扣（全天）',
    start_date      DATE            NOT NULL COMMENT '活动开始日期（含）',
    end_date        DATE            NOT NULL COMMENT '活动结束日期（含）',
    status          VARCHAR(8)      NOT NULL DEFAULT 'on' COMMENT 'on/off',
    remark          VARCHAR(255)   NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀/限时折扣活动';

-- 3) 活动商品（一行一 SKU）。seckill_num 为秒杀限量，sold 为已售（DB），Redis 预扣为快路径。
CREATE TABLE `seckill_goods` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    active_id       BIGINT UNSIGNED NOT NULL,
    goods_id        BIGINT UNSIGNED NOT NULL,
    sku_id          BIGINT UNSIGNED NOT NULL,
    seckill_price   DECIMAL(10,2)   NOT NULL COMMENT '秒杀价/限时折扣价',
    seckill_num     INT             NOT NULL DEFAULT 0 COMMENT '秒杀限量（0=不限，仅用于秒杀；限时折扣按普通库存）',
    limit_per_user  INT             NOT NULL DEFAULT 0 COMMENT '每人限购，0=不限',
    sold            INT             NOT NULL DEFAULT 0 COMMENT '已售（DB），对账任务据此与 Redis 对齐',
    status          VARCHAR(8)      NOT NULL DEFAULT 'on' COMMENT 'on/off',
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_active_sku (shop_id, active_id, sku_id),
    KEY idx_active (active_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀/限时折扣活动商品';
