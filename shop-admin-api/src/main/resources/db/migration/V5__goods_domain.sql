-- ============================================================
-- V5 商品域：分类/品牌/规格/商品/SKU/服务保障
-- 见文档三 §3.2；文档二 §2.2 商品管理
-- 这批表带 shop_id，走 ShopTenantLineHandler 的正常隔离路径
-- ============================================================

CREATE TABLE goods_category (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    parent_id       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    name            VARCHAR(64)     NOT NULL,
    image           VARCHAR(255)    NULL,
    sort            INT             NOT NULL DEFAULT 0,
    is_show         TINYINT         NOT NULL DEFAULT 1,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_parent (shop_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类（三级树）';

CREATE TABLE goods_brand (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    logo            VARCHAR(255)    NULL,
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品品牌';

CREATE TABLE goods_service (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL COMMENT '如：七天无理由退换、假一赔十',
    icon            VARCHAR(255)    NULL,
    intro           VARCHAR(255)    NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品服务保障（购物车/详情页展示的标签）';

CREATE TABLE goods_spec (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(32)     NOT NULL COMMENT '规格名，如：颜色、尺码',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格库（规格名，可跨商品复用，如"颜色"）';

CREATE TABLE goods_spec_value (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    spec_id         BIGINT UNSIGNED NOT NULL,
    value           VARCHAR(32)     NOT NULL COMMENT '规格值，如：红色',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_spec (shop_id, spec_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格值';

CREATE TABLE goods (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    category_ids        JSON            NOT NULL COMMENT '所属分类ID集合，支持多选',
    brand_id            BIGINT UNSIGNED NULL,
    name                VARCHAR(128)    NOT NULL,
    sub_name            VARCHAR(255)    NULL COMMENT '副标题',
    code                VARCHAR(64)     NULL COMMENT '商品编码',
    images              JSON            NOT NULL COMMENT '商品主图，第一张为封面',
    video               VARCHAR(255)    NULL,
    spec_type           VARCHAR(8)      NOT NULL DEFAULT 'single' COMMENT 'single单规格 / multi多规格',
    content             LONGTEXT        NULL COMMENT '商品详情图文',
    status              VARCHAR(8)      NOT NULL DEFAULT 'off' COMMENT 'on出售中 / off仓库中 / deleted回收站',
    sales_initial       INT             NOT NULL DEFAULT 0 COMMENT '初始销量基数（用于展示，不参与真实统计）',
    sales_actual        INT             NOT NULL DEFAULT 0 COMMENT '真实销量',
    stock_total         INT             NOT NULL DEFAULT 0 COMMENT '库存汇总（各SKU库存之和，冗余字段便于列表展示排序）',
    delivery_type       JSON            NOT NULL COMMENT '配送方式，如 ["express"] 或 ["express","pickup"]',
    freight_template_id BIGINT UNSIGNED NULL,
    freight_fee         DECIMAL(10,2)   NULL COMMENT '统一运费（不使用运费模板时）',
    service_ids         JSON            NULL COMMENT '关联的服务保障ID集合',
    is_virtual          TINYINT         NOT NULL DEFAULT 0 COMMENT '是否虚拟商品（无需物流）',
    limit_type          VARCHAR(8)      NOT NULL DEFAULT 'none' COMMENT 'none不限购 / single单次限购',
    limit_num           INT             NULL,
    sort                INT             NOT NULL DEFAULT 0,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_status (shop_id, status),
    KEY idx_shop_brand (shop_id, brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品主表';

CREATE TABLE goods_sku (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    goods_id        BIGINT UNSIGNED NOT NULL,
    sku_code        VARCHAR(64)     NULL,
    -- 单规格商品的 spec_value_ids 为空字符串，走与多规格完全相同的下单逻辑，不单独分叉，见文档三 §3.2
    spec_value_ids  VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '有序规格值ID串，如 "12_35"；单规格商品固定为空串',
    price           DECIMAL(10,2)   NOT NULL,
    line_price      DECIMAL(10,2)   NULL COMMENT '划线价',
    cost_price      DECIMAL(10,2)   NULL,
    stock           INT             NOT NULL DEFAULT 0,
    weight          DECIMAL(10,3)   NULL COMMENT '重量(kg)，按重量计费运费模板用',
    volume          DECIMAL(10,3)   NULL COMMENT '体积(m³)，按体积计费运费模板用',
    image           VARCHAR(255)    NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_goods_spec (shop_id, goods_id, spec_value_ids),
    KEY idx_shop_goods (shop_id, goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU（单规格商品也生成唯一一条SKU）';
