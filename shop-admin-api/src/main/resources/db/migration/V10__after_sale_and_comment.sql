-- 售后单：仅退款(refund_only) / 退货退款(return_refund)。见文档三 §3.3、开发计划 Sprint 6。
-- 退款金额不重新走价格引擎计算，而是按 order_goods.discount_detail 记录的分摊金额逆向计算
-- （原价占比 * 已付分摊金额）——保证"预览多少、实退多少"口径与下单时完全一致。
CREATE TABLE after_sale (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    order_goods_id  BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    type            VARCHAR(16)     NOT NULL COMMENT 'refund_only仅退款 / return_refund退货退款',
    apply_reason    VARCHAR(64)     NOT NULL,
    apply_desc      VARCHAR(255)    NULL,
    images          JSON            NULL COMMENT 'JSON数组：凭证图片',
    refund_num      INT             NOT NULL COMMENT '本次退款件数，不能超过 order_goods.total_num',
    refund_amount   DECIMAL(10,2)   NOT NULL COMMENT '按分摊比例逆算的退款金额，见 RefundCalculator',
    refund_detail   JSON            NULL COMMENT '退款明细分摊，如 {"coupon":2.60,"points":0.50}，结构与 order_goods.discount_detail 对应',
    status          VARCHAR(16)     NOT NULL DEFAULT 'applying' COMMENT 'applying审核中/approved已同意/rejected已拒绝/return_shipped买家已退货/refunding退款中/refunded已退款/closed已关闭',
    audit_remark    VARCHAR(255)    NULL,
    return_express_company VARCHAR(64) NULL COMMENT '退货退款场景：买家退货的快递公司',
    return_express_no       VARCHAR(64) NULL,
    refund_no       VARCHAR(64)     NULL COMMENT '本平台生成的退款单号，传给微信退款接口作 out_refund_no',
    wx_refund_id    VARCHAR(64)     NULL COMMENT '微信支付侧退款单号',
    refund_time     DATETIME        NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_shop_order (shop_id, order_id),
    KEY idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后单';

-- 退款流水：一条 after_sale 可能对应多次退款调用尝试（失败重试），流水表独立记录每次调用结果，
-- after_sale.wx_refund_id/refund_time 只保存"最终成功"的那一次，历史尝试记录在这里方便排查。
CREATE TABLE refund_log (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    after_sale_id   BIGINT UNSIGNED NOT NULL,
    refund_no       VARCHAR(64)     NOT NULL,
    amount          DECIMAL(10,2)   NOT NULL,
    status          VARCHAR(16)     NOT NULL COMMENT 'success/closed/processing/abnormal/failed',
    raw_response    TEXT            NULL COMMENT '微信退款接口原始返回，排查用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_after_sale (shop_id, after_sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款调用流水';

-- 商品评价：仅允许已完成收货的订单发布，一个 order_goods 只能评价一次（见 order_goods.is_comment 标记位）。
CREATE TABLE goods_comment (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    goods_id        BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    order_goods_id  BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    score           TINYINT         NOT NULL COMMENT '1~5星',
    content         VARCHAR(500)    NULL,
    images          JSON            NULL COMMENT 'JSON数组',
    reply           VARCHAR(500)    NULL COMMENT '商户回复',
    status          VARCHAR(16)     NOT NULL DEFAULT 'show' COMMENT 'show展示中/hidden已隐藏',
    is_top          TINYINT         NOT NULL DEFAULT 0,
    append_content  VARCHAR(500)    NULL COMMENT '追评内容',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_goods (order_goods_id),
    KEY idx_shop_goods (shop_id, goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价';
