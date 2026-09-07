-- 发货包裹：支持批量发货（一次操作多个订单）与部分发货（一个订单拆多个包裹），见文档三 §3.3、开发计划 Sprint 5。
CREATE TABLE order_package (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    express_company VARCHAR(64)     NOT NULL COMMENT '快递公司名称/编码',
    express_no      VARCHAR(64)     NOT NULL COMMENT '快递单号',
    -- JSON数组：本包裹包含的 order_goods.id 列表；单包裹发货时包含该订单全部行，
    -- 部分发货时只包含本次实际发出的行——不建单独的中间表，JSON 数组已足够表达且省一次 JOIN。
    order_goods_ids TEXT            NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_order (shop_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单发货包裹（支持部分发货/多包裹）';
