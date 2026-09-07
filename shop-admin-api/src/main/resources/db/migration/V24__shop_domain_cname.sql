-- Sprint 16：自定义域名 CNAME 校验与审核驳回原因。
-- Let's Encrypt 自动签发仍按排期后续接入；本列先记录解析状态，审核通过后 cert_status 记为 valid 占位。

ALTER TABLE shop_domain
    ADD COLUMN cname_target   VARCHAR(128) NULL COMMENT '要求商家 CNAME 到的平台域名，如 demo.shop.com' AFTER verify_status,
    ADD COLUMN cname_status   VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/ok/fail/skipped' AFTER cname_target,
    ADD COLUMN reject_reason  VARCHAR(255) NULL AFTER cname_status;

UPDATE shop_domain SET cname_status = 'ok' WHERE type = 'sub';
