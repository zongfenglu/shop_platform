ALTER TABLE shop_domain
    ADD COLUMN protocol VARCHAR(8) NOT NULL DEFAULT 'http' COMMENT '访问协议：http/https' AFTER domain;
