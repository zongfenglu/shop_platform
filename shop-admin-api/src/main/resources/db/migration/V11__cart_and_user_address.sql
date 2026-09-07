-- ============================================================
-- V11 消费者端交易前置：购物车 / 用户地址簿
-- 见开发计划 Sprint 4「购物车 API、地址管理」；文档三 §3.4 会员域 user_address
-- 两张表都带 shop_id，走 ShopTenantLineHandler 的正常隔离路径。
-- ============================================================

-- 购物车按 SKU 维度存储：同一商品的不同规格是两条独立记录，与下单链路的"只认 SKU"保持一致
-- （见 goods_sku 表注释）。不存价格快照——购物车展示价一律实时读 goods_sku.price，
-- 否则商家改价后购物车会长期显示旧价，结算时又跳成新价，是电商最典型的客诉来源。
CREATE TABLE cart (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    goods_id        BIGINT UNSIGNED NOT NULL,
    sku_id          BIGINT UNSIGNED NOT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    -- 逻辑删除下唯一键会拦住"删掉后重新加购同一SKU"，因此这里只建普通索引，
    -- 加购去重由 CartService 的"先查后改数量"负责（并发重复只会多出一行，不影响正确性）。
    KEY idx_shop_user (shop_id, user_id),
    KEY idx_shop_user_sku (shop_id, user_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车（按SKU维度）';

-- 地址簿。下单时会复制一份快照到 order_address（见 V6），用户之后改地址簿不影响历史订单。
-- 省市区这里用三个独立字段而不是文档三里写的单个 region，与 order_address 的列保持一一对应，
-- 避免下单快照时要做字符串拆分。
CREATE TABLE user_address (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    phone           VARCHAR(20)     NOT NULL,
    province        VARCHAR(32)     NULL,
    city            VARCHAR(32)     NULL,
    region          VARCHAR(32)     NULL,
    detail          VARCHAR(255)    NOT NULL,
    is_default      TINYINT         NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_user (shop_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址簿';
