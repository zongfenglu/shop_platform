-- 将原微信专用配置升级为多支付渠道配置。现有微信记录原样保留。
ALTER TABLE shop_pay_config
    MODIFY COLUMN channel VARCHAR(32) NOT NULL COMMENT '支付渠道：wechat/alipay',
    MODIFY COLUMN app_id VARCHAR(64) NULL COMMENT '渠道应用ID',
    MODIFY COLUMN mch_id VARCHAR(64) NULL COMMENT '微信商户号',
    MODIFY COLUMN mch_cert_serial_no VARCHAR(64) NULL COMMENT '微信商户API证书序列号',
    MODIFY COLUMN api_v3_key_encrypted VARCHAR(255) NULL COMMENT '微信APIv3密钥（AES-256-GCM）',
    MODIFY COLUMN mch_private_key_encrypted TEXT NULL COMMENT '渠道应用/商户私钥（AES-256-GCM）',
    ADD COLUMN alipay_public_key_encrypted TEXT NULL COMMENT '支付宝公钥（AES-256-GCM）' AFTER mch_private_key_encrypted,
    ADD COLUMN gateway_url VARCHAR(255) NULL COMMENT '渠道网关地址，支付宝默认正式网关' AFTER alipay_public_key_encrypted,
    ADD COLUMN sort_no INT NOT NULL DEFAULT 100 COMMENT 'H5支付方式排序' AFTER gateway_url;

ALTER TABLE pay_notify_log
    DROP INDEX uk_shop_transaction,
    ADD UNIQUE KEY uk_shop_channel_transaction (shop_id, channel, transaction_id);

UPDATE shop_pay_config SET sort_no = 10 WHERE channel = 'wechat';
