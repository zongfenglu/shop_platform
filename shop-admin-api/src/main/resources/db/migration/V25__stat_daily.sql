-- T+1 日报。见文档三 §3.7 / §6。uv/pv 暂无埋点，任务写 0。
CREATE TABLE stat_shop_daily (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    stat_date       DATE            NOT NULL,
    order_count     INT             NOT NULL DEFAULT 0,
    pay_count       INT             NOT NULL DEFAULT 0,
    pay_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    refund_amount   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    new_user        INT             NOT NULL DEFAULT 0,
    active_user     INT             NOT NULL DEFAULT 0,
    uv              INT             NOT NULL DEFAULT 0,
    pv              INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shop_date (shop_id, stat_date),
    KEY idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺日报';

CREATE TABLE stat_goods_daily (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    goods_id        BIGINT UNSIGNED NOT NULL,
    stat_date       DATE            NOT NULL,
    views           INT             NOT NULL DEFAULT 0,
    add_cart        INT             NOT NULL DEFAULT 0,
    pay_count       INT             NOT NULL DEFAULT 0,
    pay_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shop_goods_date (shop_id, goods_id, stat_date),
    KEY idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品日报';
