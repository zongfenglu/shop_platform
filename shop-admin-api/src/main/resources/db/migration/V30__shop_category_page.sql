-- 分类页模板：每店一条，控制 C 端分类 Tab 的版式与分享标题。
-- 见原型 store/diy-page-list.html「分类页样式」；对照萤火式 一级大图 / 一级小图 / 二级分类。
CREATE TABLE shop_category_page (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    style           VARCHAR(32)     NOT NULL DEFAULT 'level1_small' COMMENT 'level1_large / level1_small / level2',
    share_title     VARCHAR(64)     NULL COMMENT '分享标题，空则用全部分类',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城分类页模板';
