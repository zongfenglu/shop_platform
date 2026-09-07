-- 商户支付配置：租户自有微信商户号，敏感字段（API v3密钥、商户私钥）加密落库，见文档三 §9。
CREATE TABLE shop_pay_config (
    id                      BIGINT UNSIGNED PRIMARY KEY,
    shop_id                 BIGINT UNSIGNED NOT NULL,
    channel                 VARCHAR(16)     NOT NULL DEFAULT 'wechat' COMMENT '支付渠道，当前仅微信支付',
    app_id                  VARCHAR(64)     NOT NULL COMMENT '微信支付APPID（公众号/小程序）',
    mch_id                  VARCHAR(64)     NOT NULL COMMENT '微信支付商户号',
    mch_cert_serial_no      VARCHAR(64)     NOT NULL COMMENT '商户API证书序列号，非敏感信息',
    api_v3_key_encrypted    VARCHAR(255)    NOT NULL COMMENT 'APIv3密钥，AES-256-GCM加密存储',
    mch_private_key_encrypted TEXT         NOT NULL COMMENT '商户API私钥(PEM)，AES-256-GCM加密存储',
    status                  VARCHAR(16)     NOT NULL DEFAULT 'enabled' COMMENT 'enabled/disabled',
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete                TINYINT        NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_channel (shop_id, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户支付配置（租户自有商户号）';

-- 支付回调幂等日志：以 (shop_id, transaction_id) 唯一约束兜底并发重复投递，见文档三 §9。
CREATE TABLE pay_notify_log (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    channel         VARCHAR(16)     NOT NULL,
    transaction_id  VARCHAR(64)     NOT NULL COMMENT '支付渠道侧交易号',
    out_trade_no    VARCHAR(32)     NOT NULL COMMENT '对应 order.order_no',
    process_result  VARCHAR(16)     NOT NULL COMMENT 'success/duplicate/failed',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_transaction (shop_id, transaction_id),
    KEY idx_out_trade_no (out_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调幂等日志';
