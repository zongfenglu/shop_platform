-- Sprint 10: sign-in + points mall. See doc 03 sec 3.5 marketing domain.
-- sign_config: per-shop singleton (daily_points + continuous_rules JSON).
-- sign_record: one row per (shop_id, user_id, sign_date) unique; day_number snapshots streak position.
--   Streak resets to 1 on break; continuous_rules auto-awarded on exact day match.
--   is_makeup=1 restores streak only (no points issued); makeup-card inventory deferred.
-- points_goods: exchange items (coupon_id XOR goods_id). points+cash pricing, stock (0=unlimited).
-- exchange_record: two-step create (validate-only) then pay (atomic stock decrement + atomic points deduction).

-- 1) Sign-in config
CREATE TABLE `sign_config` (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    daily_points        INT             NOT NULL DEFAULT 2 COMMENT 'daily sign-in points',
    continuous_rules    JSON            NULL COMMENT '[{"days":3,"type":"points","value":5},{"days":7,"type":"coupon","couponId":123}] consecutive reward tiers',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sign-in config';

-- 2) Sign-in record
CREATE TABLE `sign_record` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    sign_date       DATE            NOT NULL,
    day_number      INT             NOT NULL COMMENT 'streak day number (1=first day, resets on break)',
    points_earned   INT             NOT NULL DEFAULT 0 COMMENT 'points earned including milestone bonus',
    is_makeup       TINYINT         NOT NULL DEFAULT 0 COMMENT '0=normal 1=makeup (no points)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_user_date (shop_id, user_id, sign_date),
    KEY idx_shop_user (shop_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sign-in record';

-- 3) Points mall exchange items
CREATE TABLE `points_goods` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL COMMENT 'display name',
    image           VARCHAR(255)    NULL,
    goods_id        BIGINT UNSIGNED NULL COMMENT 'physical goods.id (fulfillment type)',
    coupon_id       BIGINT UNSIGNED NULL COMMENT 'coupon.id (instant-issue type)',
    points          INT             NOT NULL COMMENT 'points cost',
    cash            DECIMAL(10,2)   NULL COMMENT 'cash portion for points+cash mode, NULL/0=pure points',
    stock           INT             NOT NULL DEFAULT 0 COMMENT 'stock, 0=unlimited',
    status          VARCHAR(8)      NOT NULL DEFAULT 'on' COMMENT 'on/off',
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='points mall exchange items';

-- 4) Exchange record (fulfillment queue + cash payment reference)
CREATE TABLE `exchange_record` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    points_goods_id BIGINT UNSIGNED NOT NULL,
    goods_type      VARCHAR(8)      NOT NULL COMMENT 'coupon (instant issue) / goods (fulfillment)',
    coupon_id       BIGINT UNSIGNED NULL,
    user_coupon_id  BIGINT UNSIGNED NULL COMMENT 'issued user_coupon.id when goods_type=coupon',
    points_cost     INT             NOT NULL COMMENT 'points deducted',
    cash_price      DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT 'cash portion, 0=pure points',
    pay_status      VARCHAR(10)     NOT NULL DEFAULT 'unpaid' COMMENT 'unpaid/paid',
    status          VARCHAR(10)     NOT NULL DEFAULT 'pending' COMMENT 'pending (awaiting fulfillment) / fulfilled (goods type)',
    name            VARCHAR(64)     NULL COMMENT 'snapshot: item name at exchange time',
    image           VARCHAR(255)    NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_user (shop_id, user_id),
    KEY idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='points exchange record';
