-- Sprint 11: dealer/distribution core. See doc 03 sec 3.6 dealer domain.
-- dealer_setting: per-shop singleton config (commission rate, min withdraw, auto-approve).
-- dealer_user: user's dealer profile with referral tree (parent_id).
--   Status: applying -> store approves -> active (or rejected/disabled).
-- dealer_order: commission record per eligible order, created on payment success, settled after return period.

CREATE TABLE `dealer_setting` (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    is_enable           TINYINT         NOT NULL DEFAULT 0 COMMENT '0=disabled 1=enabled',
    commission_rate     DECIMAL(5,2)    NOT NULL DEFAULT 10.00 COMMENT 'commission rate in percent, e.g. 10.00 = 10%',
    commission_type     VARCHAR(10)     NOT NULL DEFAULT 'order' COMMENT 'order (entire order) / goods (per item)',
    min_withdraw        DECIMAL(10,2)   NOT NULL DEFAULT 10.00 COMMENT 'minimum withdraw amount',
    auto_approve        TINYINT         NOT NULL DEFAULT 0 COMMENT '0=manual review 1=auto approve applications',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='dealer settings';

CREATE TABLE `dealer_user` (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    parent_id           BIGINT UNSIGNED NULL COMMENT 'upstream dealer (referrer), forms referral tree',
    status              VARCHAR(10)     NOT NULL DEFAULT 'applying' COMMENT 'applying/active/disabled/rejected',
    real_name           VARCHAR(32)     NULL,
    mobile              VARCHAR(20)     NULL,
    total_commission    DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    available_commission DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    frozen_commission   DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    apply_time          DATETIME        NULL,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_user (shop_id, user_id),
    KEY idx_shop_parent (shop_id, parent_id),
    KEY idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='dealer user profile';

CREATE TABLE `dealer_order` (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    order_id            BIGINT UNSIGNED NOT NULL,
    dealer_user_id      BIGINT UNSIGNED NOT NULL,
    order_total         DECIMAL(10,2)   NOT NULL COMMENT 'order total amount (snapshot)',
    commission_rate     DECIMAL(5,2)    NOT NULL COMMENT 'commission rate at order time',
    commission_amount   DECIMAL(10,2)   NOT NULL COMMENT 'calculated commission',
    status              VARCHAR(10)     NOT NULL DEFAULT 'pending' COMMENT 'pending/settled/refunded',
    settle_time         DATETIME        NULL COMMENT 'when commission became available',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_dealer (shop_id, dealer_user_id),
    KEY idx_shop_status (shop_id, status),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='dealer commission records';
