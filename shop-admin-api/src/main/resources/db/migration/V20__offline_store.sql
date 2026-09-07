-- Sprint 13: offline store self-pickup (门店自提). See doc 03 sec §? / sprint plan S13.
-- offline_store: merchant's physical pickup locations.
-- verify_log: audit trail of self-pickup verification, one row per successfully verified order
--             (unique on order_id doubles as a defense-in-depth guard against double verification,
--             on top of the optimistic condition update on order.delivery_status).
-- order.pickup_store_id / pickup_code: added to the existing order table for delivery_type='pickup' orders.

CREATE TABLE `offline_store` (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    name                VARCHAR(64)     NOT NULL,
    logo                VARCHAR(255)    NULL,
    phone               VARCHAR(20)     NULL,
    region              VARCHAR(255)    NULL COMMENT 'province/city/district, JSON or plain text',
    detail              VARCHAR(255)    NULL COMMENT 'street address detail',
    longitude           DECIMAL(10,6)   NULL,
    latitude            DECIMAL(10,6)   NULL,
    business_hours      VARCHAR(64)     NULL,
    status              VARCHAR(10)     NOT NULL DEFAULT 'enabled' COMMENT 'enabled/disabled',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='offline pickup stores';

CREATE TABLE `verify_log` (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    order_id            BIGINT UNSIGNED NOT NULL,
    store_id            BIGINT UNSIGNED NOT NULL,
    clerk_id            BIGINT UNSIGNED NOT NULL COMMENT 'store_user.id of the operator who verified',
    verify_code         VARCHAR(8)      NOT NULL COMMENT 'snapshot of the code used, for audit',
    verify_time         DATETIME        NOT NULL,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order (order_id),
    KEY idx_shop_store (shop_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='self-pickup verification records';

ALTER TABLE `order`
    ADD COLUMN pickup_store_id BIGINT UNSIGNED NULL COMMENT 'offline_store.id, set when delivery_type=pickup' AFTER delivery_type,
    ADD COLUMN pickup_code     VARCHAR(8)      NULL COMMENT 'self-pickup verification code, generated at order creation' AFTER pickup_store_id,
    ADD KEY idx_shop_pickup_code (shop_id, pickup_code);
