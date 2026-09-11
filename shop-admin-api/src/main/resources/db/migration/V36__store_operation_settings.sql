-- Merchant operation settings: logistics, returns, uploads, receipt printers and SMS channels.

CREATE TABLE store_operation_setting (
    id                              BIGINT UNSIGNED PRIMARY KEY,
    shop_id                         BIGINT UNSIGNED NOT NULL,
    upload_provider                 VARCHAR(24) NOT NULL DEFAULT 'local',
    upload_bucket                   VARCHAR(128) NULL,
    upload_region                   VARCHAR(64) NULL,
    upload_endpoint                 VARCHAR(255) NULL,
    upload_domain                   VARCHAR(255) NULL,
    upload_access_key_id_encrypted  TEXT NULL,
    upload_access_key_secret_encrypted TEXT NULL,
    image_max_mb                    INT NOT NULL DEFAULT 5,
    video_max_mb                    INT NOT NULL DEFAULT 50,
    print_enabled                   TINYINT NOT NULL DEFAULT 0,
    print_printer_id                BIGINT UNSIGNED NULL,
    print_on_paid                   TINYINT NOT NULL DEFAULT 1,
    print_on_refund                 TINYINT NOT NULL DEFAULT 0,
    print_copies                    INT NOT NULL DEFAULT 1,
    sms_enabled                     TINYINT NOT NULL DEFAULT 0,
    sms_new_order_template          VARCHAR(128) NULL,
    sms_paid_template               VARCHAR(128) NULL,
    sms_shipped_template            VARCHAR(128) NULL,
    sms_refund_template             VARCHAR(128) NULL,
    sms_notify_phones               VARCHAR(512) NULL,
    create_time                     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete                       TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='merchant upload, print and SMS rules';

CREATE TABLE express_company (
    id          BIGINT UNSIGNED PRIMARY KEY,
    shop_id     BIGINT UNSIGNED NOT NULL,
    name        VARCHAR(64) NOT NULL,
    code        VARCHAR(32) NOT NULL,
    sort        INT NOT NULL DEFAULT 100,
    status      VARCHAR(10) NOT NULL DEFAULT 'enabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete   TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_code (shop_id, code),
    KEY idx_shop_status_sort (shop_id, status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='merchant express companies';

CREATE TABLE return_address (
    id           BIGINT UNSIGNED PRIMARY KEY,
    shop_id      BIGINT UNSIGNED NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    phone        VARCHAR(20) NOT NULL,
    province     VARCHAR(32) NOT NULL,
    city         VARCHAR(32) NOT NULL,
    district     VARCHAR(32) NOT NULL,
    detail       VARCHAR(255) NOT NULL,
    postal_code  VARCHAR(12) NULL,
    is_default   TINYINT NOT NULL DEFAULT 0,
    sort         INT NOT NULL DEFAULT 100,
    status       VARCHAR(10) NOT NULL DEFAULT 'enabled',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete    TINYINT NOT NULL DEFAULT 0,
    KEY idx_shop_default (shop_id, is_default, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='merchant return addresses';

CREATE TABLE receipt_printer (
    id                          BIGINT UNSIGNED PRIMARY KEY,
    shop_id                     BIGINT UNSIGNED NOT NULL,
    name                        VARCHAR(64) NOT NULL,
    provider                    VARCHAR(24) NOT NULL,
    device_no                   VARCHAR(128) NOT NULL,
    access_key_encrypted        TEXT NULL,
    access_secret_encrypted     TEXT NULL,
    endpoint                    VARCHAR(255) NULL,
    sort                        INT NOT NULL DEFAULT 100,
    status                      VARCHAR(10) NOT NULL DEFAULT 'enabled',
    create_time                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete                   TINYINT NOT NULL DEFAULT 0,
    KEY idx_shop_status_sort (shop_id, status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='merchant receipt printers';

CREATE TABLE sms_channel (
    id                          BIGINT UNSIGNED PRIMARY KEY,
    shop_id                     BIGINT UNSIGNED NOT NULL,
    name                        VARCHAR(64) NOT NULL,
    provider                    VARCHAR(24) NOT NULL,
    app_id                      VARCHAR(128) NULL,
    access_key_id_encrypted     TEXT NULL,
    access_key_secret_encrypted TEXT NULL,
    sign_name                   VARCHAR(64) NULL,
    endpoint                    VARCHAR(255) NULL,
    priority                    INT NOT NULL DEFAULT 100,
    status                      VARCHAR(10) NOT NULL DEFAULT 'enabled',
    create_time                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete                   TINYINT NOT NULL DEFAULT 0,
    KEY idx_shop_status_priority (shop_id, status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='merchant SMS channels';

-- Seed existing merchants with MySQL-generated 64-bit IDs. New/empty merchants are also covered lazily by the service.
INSERT INTO express_company (id, shop_id, name, code, sort, status)
SELECT UUID_SHORT(), shop.id, d.name, d.code, d.sort, 'enabled'
FROM shop
JOIN (
    SELECT '顺丰速运' name, 'shunfeng' code, 10 sort UNION ALL
    SELECT '京东物流', 'jd', 20 UNION ALL
    SELECT '中通快递', 'zhongtong', 30 UNION ALL
    SELECT '圆通速递', 'yuantong', 40 UNION ALL
    SELECT '申通快递', 'shentong', 50 UNION ALL
    SELECT '韵达快递', 'yunda', 60 UNION ALL
    SELECT '邮政EMS', 'ems', 70 UNION ALL
    SELECT '极兔速递', 'jtexpress', 80
) d ON 1 = 1
WHERE shop.is_delete = 0;

ALTER TABLE after_sale
    ADD COLUMN return_address_snapshot JSON NULL COMMENT 'return address selected when merchant approves' AFTER audit_remark;
