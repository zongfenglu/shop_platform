-- 会员（消费者）表。见开发计划 Sprint 4/Sprint 7：
-- Sprint 4 先落地登录与下单必需的最小字段；等级/积分/余额相关列由 Sprint 7 会员体系 ALTER TABLE 补齐，
-- 不新建第二张表，避免 order.user_id 将来面临"到底指向哪张用户表"的歧义。
CREATE TABLE `user` (
    id          BIGINT UNSIGNED PRIMARY KEY,
    shop_id     BIGINT UNSIGNED NOT NULL,
    mobile      VARCHAR(20)     NOT NULL,
    nickname    VARCHAR(64)     NULL,
    avatar      VARCHAR(255)    NULL,
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete   TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_mobile (shop_id, mobile),
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城会员（消费者）';
