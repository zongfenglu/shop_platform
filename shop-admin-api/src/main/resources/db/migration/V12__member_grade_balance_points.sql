-- Sprint 7 会员体系：等级 / 余额 / 积分 / 成长值 / 充值。
-- 见文档三 §3.4 会员域。V7 已建好 `user` 表的最小先行版本（登录/下单必需字段），
-- 本迁移按文档三注释直接在 `user` 上 ALTER TABLE 补齐，不新建第二张用户表，
-- 避免 order.user_id 将来面临"到底指向哪张用户表"的歧义。

-- 1) 扩展 user 表：等级 / 余额 / 积分 / 成长值 / 消费统计 / 黑名单 / 第三方身份
ALTER TABLE `user`
    ADD COLUMN gender          TINYINT         NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    ADD COLUMN platform        VARCHAR(20)     NULL COMMENT '来源端 mp/h5/app/mp-official',
    ADD COLUMN open_id         VARCHAR(64)     NULL COMMENT '微信 openId（按 (shop_id,open_id) 唯一，同微信用户在不同租户是独立账号）',
    ADD COLUMN union_id        VARCHAR(64)     NULL,
    ADD COLUMN balance         DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    ADD COLUMN points          INT             NOT NULL DEFAULT 0    COMMENT '可用积分',
    ADD COLUMN growth_value    INT             NOT NULL DEFAULT 0    COMMENT '成长值，等级升级依据',
    ADD COLUMN grade_id        BIGINT UNSIGNED NULL COMMENT '当前会员等级 user_grade.id',
    ADD COLUMN pay_money       DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '累计实付金额',
    ADD COLUMN pay_count       INT             NOT NULL DEFAULT 0    COMMENT '累计成交订单数',
    ADD COLUMN last_login_time DATETIME        NULL,
    ADD COLUMN is_black        TINYINT         NOT NULL DEFAULT 0 COMMENT '是否拉黑 0否 1是',
    ADD INDEX idx_shop_grade (shop_id, grade_id);

-- 2) 会员等级。每个租户自配，建店时由 ShopServiceImpl 灌入默认等级。
CREATE TABLE `user_grade` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    weight          INT             NOT NULL DEFAULT 0  COMMENT '排序权重，越大等级越高',
    growth_value    INT             NOT NULL DEFAULT 0  COMMENT '达到该等级所需成长值',
    discount_ratio  DECIMAL(3,2)   NOT NULL DEFAULT 1.00 COMMENT '会员折扣 1.00=无折扣 0.90=九折',
    icon            VARCHAR(255)    NULL,
    remark          VARCHAR(255)    NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级';

-- 3) 余额变动流水。money 带正负号：充值+/消费-/退款+/后台调整±/佣金+。
CREATE TABLE `user_balance_log` (
    id          BIGINT UNSIGNED PRIMARY KEY,
    shop_id     BIGINT UNSIGNED NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL,
    scene       VARCHAR(20)     NOT NULL COMMENT 'recharge/consume/refund/admin/commission',
    money       DECIMAL(10,2)   NOT NULL COMMENT '变动金额，带正负号',
    `before`    DECIMAL(10,2)   NOT NULL,
    `after`     DECIMAL(10,2)   NOT NULL,
    remark      VARCHAR(255)    NULL,
    order_id    BIGINT UNSIGNED NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete   TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_user (shop_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员余额变动流水';

-- 4) 积分变动流水。value 带正负号。
CREATE TABLE `user_points_log` (
    id          BIGINT UNSIGNED PRIMARY KEY,
    shop_id     BIGINT UNSIGNED NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL,
    scene       VARCHAR(20)     NOT NULL COMMENT 'recharge/consume/refund/admin/sign/order',
    value       INT             NOT NULL COMMENT '变动积分，带正负号',
    `before`    INT             NOT NULL,
    `after`     INT             NOT NULL,
    remark      VARCHAR(255)    NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete   TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_user (shop_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员积分变动流水';

-- 5) 充值方案。
CREATE TABLE `recharge_plan` (
    id          BIGINT UNSIGNED PRIMARY KEY,
    shop_id     BIGINT UNSIGNED NOT NULL,
    money       DECIMAL(10,2)   NOT NULL COMMENT '用户实付金额',
    gift_money  DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '赠送余额',
    gift_points INT             NOT NULL DEFAULT 0    COMMENT '赠送积分',
    is_show     TINYINT         NOT NULL DEFAULT 1,
    sort        INT             NOT NULL DEFAULT 0,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete   TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额充值方案';

-- 6) 充值订单。pay_status: unpaid/paid。支付成功后由 RechargeOrderService 入账（余额+赠送、积分+赠送）。
CREATE TABLE `recharge_order` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    order_no        VARCHAR(32)     NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    plan_id         BIGINT UNSIGNED NOT NULL,
    pay_price       DECIMAL(10,2)   NOT NULL,
    gift_money      DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    gift_points     INT             NOT NULL DEFAULT 0,
    pay_status      VARCHAR(10)     NOT NULL DEFAULT 'unpaid',
    pay_method      VARCHAR(20)     NULL,
    pay_time        DATETIME        NULL,
    transaction_id  VARCHAR(64)     NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_order_no (shop_id, order_no),
    KEY idx_shop_user (shop_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员充值订单';
