-- 套餐订购发票 + 运维备份记录。见文档二 §1.4 / §1.9。
CREATE TABLE shop_invoice (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    shop_order_id   BIGINT UNSIGNED NOT NULL,
    title           VARCHAR(128)    NOT NULL COMMENT '发票抬头',
    tax_no          VARCHAR(32)     NOT NULL COMMENT '税号',
    amount          DECIMAL(12, 2)  NOT NULL,
    status          VARCHAR(16)     NOT NULL COMMENT 'applying/issued/rejected',
    invoice_no      VARCHAR(64)     NULL,
    issue_time      DATETIME        NULL,
    reject_reason   VARCHAR(255)    NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_time (shop_id, create_time),
    KEY idx_order (shop_order_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐订购发票';

CREATE TABLE ops_backup (
    id              BIGINT UNSIGNED PRIMARY KEY,
    filename        VARCHAR(128)    NOT NULL,
    file_path       VARCHAR(255)    NOT NULL,
    size_bytes      BIGINT          NOT NULL DEFAULT 0,
    status          VARCHAR(16)     NOT NULL COMMENT 'success/failed',
    message         VARCHAR(512)    NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库备份记录';
