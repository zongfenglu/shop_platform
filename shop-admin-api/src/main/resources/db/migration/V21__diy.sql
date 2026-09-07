-- Sprint 14: DIY 装修引擎（M4 装修与多端）。见 docs/04-开发计划-Sprint排期.md S14。
-- diy_page: 商户装修页面，草稿/发布双版本（page_data=已发布快照，draft_data=编辑器持续写入的草稿）。
-- diy_template: 平台级行业模板库（无 shop_id），商户"一键套用"生成自己的 diy_page。
-- diy_tabbar: 每商城全局唯一一条底部导航配置。

CREATE TABLE `diy_page` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    page_type       VARCHAR(16)     NOT NULL COMMENT 'home/custom/category/user',
    name            VARCHAR(64)     NOT NULL,
    page_data       JSON            NULL     COMMENT '已发布内容快照，从未发布过则为NULL',
    draft_data      JSON            NOT NULL COMMENT '草稿内容',
    is_default      TINYINT         NOT NULL DEFAULT 0 COMMENT '仅对page_type=home生效：是否为当前首页，同shop_id下home类型至多一条为1',
    version         INT             NOT NULL DEFAULT 0 COMMENT '发布版本号，每次publish自增1，0表示从未发布',
    publish_time    DATETIME        NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_type (shop_id, page_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装修页面（草稿/发布双版本）';

CREATE TABLE `diy_template` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    industry        VARCHAR(32)     NOT NULL COMMENT '对应 shop.industry',
    name            VARCHAR(32)     NOT NULL,
    cover           VARCHAR(255)    NULL,
    page_data       JSON            NOT NULL COMMENT '结构与 diy_page.page_data 一致',
    is_show         TINYINT         NOT NULL DEFAULT 1,
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业模板库（平台级，无shop_id）';

CREATE TABLE `diy_tabbar` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    items           JSON            NOT NULL COMMENT '[{"icon":"","activeIcon":"","text":"","path":""}]，2~5项',
    style           JSON            NULL     COMMENT '{"activeColor":"","inactiveColor":"","bgColor":""}',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='底部导航配置，每商城全局唯一一条';

-- 平台级行业模板种子数据
INSERT INTO diy_template (id, industry, name, cover, page_data, is_show, sort) VALUES
    (1, '服饰鞋包', '极简风', NULL,
        JSON_OBJECT('page', JSON_OBJECT('name', '首页', 'bgColor', '#f8f8f8'),
                     'items', JSON_ARRAY(JSON_OBJECT('type', 'search'),
                                          JSON_OBJECT('type', 'banner', 'data', JSON_ARRAY()),
                                          JSON_OBJECT('type', 'navBar', 'style', JSON_OBJECT('rowNum', 5)),
                                          JSON_OBJECT('type', 'goods', 'style', JSON_OBJECT('display', 'list', 'column', 2)))),
        1, 1),
    (2, '生鲜食品', '清新风', NULL,
        JSON_OBJECT('page', JSON_OBJECT('name', '首页', 'bgColor', '#eefaf3'),
                     'items', JSON_ARRAY(JSON_OBJECT('type', 'search'),
                                          JSON_OBJECT('type', 'banner', 'data', JSON_ARRAY()),
                                          JSON_OBJECT('type', 'goods', 'style', JSON_OBJECT('display', 'list', 'column', 2)))),
        1, 2),
    (3, '美妆个护', '国潮风', NULL,
        JSON_OBJECT('page', JSON_OBJECT('name', '首页', 'bgColor', '#fdece9'),
                     'items', JSON_ARRAY(JSON_OBJECT('type', 'search'),
                                          JSON_OBJECT('type', 'banner', 'data', JSON_ARRAY()),
                                          JSON_OBJECT('type', 'navBar', 'style', JSON_OBJECT('rowNum', 4)),
                                          JSON_OBJECT('type', 'goods', 'style', JSON_OBJECT('display', 'list', 'column', 2)))),
        1, 3),
    (4, '3C数码', '科技风', NULL,
        JSON_OBJECT('page', JSON_OBJECT('name', '首页', 'bgColor', '#eef2fb'),
                     'items', JSON_ARRAY(JSON_OBJECT('type', 'search'),
                                          JSON_OBJECT('type', 'banner', 'data', JSON_ARRAY()),
                                          JSON_OBJECT('type', 'goods', 'style', JSON_OBJECT('display', 'list', 'column', 2)))),
        1, 4);
