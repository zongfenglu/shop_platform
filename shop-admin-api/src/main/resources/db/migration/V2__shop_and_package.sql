-- ============================================================
-- V2 商城（租户）主体与套餐
-- 见文档三 §3.1；文档一 §3 租户模型、§3.4 套餐
-- ============================================================

CREATE TABLE shop (
    id              BIGINT UNSIGNED PRIMARY KEY,
    code            VARCHAR(32)     NOT NULL COMMENT '二级域名前缀，如 huajianji，全局唯一，建店时校验保留词黑名单',
    name            VARCHAR(64)     NOT NULL COMMENT '商城名称',
    logo            VARCHAR(255)    NULL,
    industry        VARCHAR(32)     NULL COMMENT '所属行业：服饰鞋包/生鲜食品/美妆个护/3C数码/其他',
    contact         VARCHAR(64)     NULL COMMENT '联系人',
    mobile          VARCHAR(20)     NULL COMMENT '联系手机',
    status          VARCHAR(16)     NOT NULL DEFAULT 'trial' COMMENT 'trial/normal/expired/disabled/archived，见 ShopStatus 枚举',
    expire_time     DATETIME        NULL COMMENT '套餐到期时间',
    package_id      BIGINT UNSIGNED NULL COMMENT '当前生效的 shop_package.id',
    remark          VARCHAR(512)    NULL COMMENT '内部备注，租户不可见',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_code (code),
    KEY idx_status (status),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城（租户）主体';

CREATE TABLE package_tpl (
    id              BIGINT UNSIGNED PRIMARY KEY,
    name            VARCHAR(32)     NOT NULL COMMENT '套餐名称：基础版/标准版/旗舰版',
    intro           VARCHAR(255)    NULL,
    menus           JSON            NOT NULL COMMENT '功能菜单集合，如 ["goods.*","marketing.coupon","marketing.seckill"]',
    quota           JSON            NOT NULL COMMENT '配额上限，如 {"goods_max":5000,"staff_max":10,"storage_mb":20480,"sms_month":1000}',
    price           JSON            NOT NULL COMMENT '价格，如 {"month":299,"quarter":799,"year":2680}',
    is_trial        TINYINT         NOT NULL DEFAULT 0 COMMENT '是否为默认试用套餐',
    is_show         TINYINT         NOT NULL DEFAULT 1 COMMENT '是否上架，供新建商城/续费时选择',
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐模板（平台侧配置，改动不影响已开通商城）';

CREATE TABLE shop_package (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    package_tpl_id  BIGINT UNSIGNED NOT NULL COMMENT '来源套餐模板，仅用于追溯，不做外键约束（模板可能已变化）',
    name            VARCHAR(32)     NOT NULL COMMENT '套餐名称快照',
    menus           JSON            NOT NULL COMMENT '功能菜单快照：开通时从 package_tpl 复制，之后平台改模板不影响本记录',
    quota           JSON            NOT NULL COMMENT '配额快照',
    price_snapshot  JSON            NOT NULL COMMENT '价格快照（用于订单追溯，不是实时价格）',
    start_time      DATETIME        NOT NULL,
    expire_time     DATETIME        NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城套餐开通记录（快照机制，见文档一§3.4）';

CREATE TABLE shop_order (
    id              BIGINT UNSIGNED PRIMARY KEY,
    order_no        VARCHAR(32)     NOT NULL COMMENT '订购单号',
    shop_id         BIGINT UNSIGNED NOT NULL,
    type            VARCHAR(16)     NOT NULL COMMENT 'new/renew/upgrade/addon',
    package_tpl_id  BIGINT UNSIGNED NULL,
    duration_month  INT             NULL COMMENT '订购周期（月）',
    amount          DECIMAL(10,2)   NOT NULL DEFAULT 0,
    pay_status      VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT 'pending/paid/refunded/closed',
    pay_method      VARCHAR(16)     NULL COMMENT 'wechat/alipay/offline',
    pay_time        DATETIME        NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订购单（新开通/续费/升级/增值服务）';

CREATE TABLE shop_domain (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    domain          VARCHAR(128)    NOT NULL COMMENT '域名，如 huajianji.shop.com 或自定义域名',
    type            VARCHAR(8)      NOT NULL COMMENT 'sub泛域名 / custom自定义域名',
    cert_status     VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT 'pending/valid/expired/failed',
    cert_expire_time DATETIME       NULL,
    verify_status   VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT 'pending/resolving/verified/rejected',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_domain (domain),
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城域名（泛域名+自定义域名）';

CREATE TABLE shop_quota_usage (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    stat_date       DATE            NOT NULL,
    goods_count     INT             NOT NULL DEFAULT 0,
    staff_count     INT             NOT NULL DEFAULT 0,
    storage_bytes   BIGINT          NOT NULL DEFAULT 0,
    sms_month_used  INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shop_date (shop_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城配额用量快照（定时任务每日写入，用于配额校验与运维中心展示）';
