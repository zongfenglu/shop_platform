-- Sprint 16 补齐：自定义域名 Let's Encrypt HTTP-01。
-- 审核通过不再把 cert_status 假装成 valid；只有 ACME 真正签发成功才写有效期与 PEM。

ALTER TABLE shop_domain
    ADD COLUMN cert_error           VARCHAR(512) NULL COMMENT '最近一次签发/续期失败原因，成功时清空' AFTER reject_reason,
    ADD COLUMN cert_pem_encrypted   TEXT         NULL COMMENT '证书+链 PEM，AES-GCM' AFTER cert_error,
    ADD COLUMN key_pem_encrypted    TEXT         NULL COMMENT '域名私钥 PEM，AES-GCM' AFTER cert_pem_encrypted;

CREATE TABLE acme_account (
    id              TINYINT         NOT NULL PRIMARY KEY,
    directory_url   VARCHAR(255)    NOT NULL DEFAULT '' COMMENT 'ACME directory，切换 staging/prod 会重建账户',
    account_url     VARCHAR(512)    NOT NULL DEFAULT '',
    contact_email   VARCHAR(128)    NOT NULL DEFAULT '',
    key_pem_encrypted TEXT          NULL COMMENT '账户私钥 PEM，AES-GCM',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Let''s Encrypt ACME 账户（平台单例）';

INSERT INTO acme_account (id, directory_url, account_url, contact_email, is_delete)
VALUES (1, '', '', '', 0);
