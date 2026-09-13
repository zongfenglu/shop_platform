-- 内容管理：文章分类、文章，以及素材文件回收站。
CREATE TABLE `content_article_category` (
    id          BIGINT UNSIGNED PRIMARY KEY,
    shop_id     BIGINT UNSIGNED NOT NULL,
    name        VARCHAR(64) NOT NULL,
    sort_no     INT NOT NULL DEFAULT 0,
    is_show     TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete   TINYINT NOT NULL DEFAULT 0,
    KEY idx_shop_sort (shop_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类';

CREATE TABLE `content_article` (
    id            BIGINT UNSIGNED PRIMARY KEY,
    shop_id       BIGINT UNSIGNED NOT NULL,
    category_id   BIGINT UNSIGNED NOT NULL,
    title         VARCHAR(120) NOT NULL,
    display_mode  VARCHAR(16) NOT NULL DEFAULT 'small' COMMENT 'small小图/large大图',
    cover_url     VARCHAR(500) NOT NULL,
    content       LONGTEXT NOT NULL,
    virtual_views INT UNSIGNED NOT NULL DEFAULT 0,
    actual_views  INT UNSIGNED NOT NULL DEFAULT 0,
    status        VARCHAR(16) NOT NULL DEFAULT 'visible' COMMENT 'visible/hidden',
    sort_no       INT NOT NULL DEFAULT 0,
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete     TINYINT NOT NULL DEFAULT 0,
    KEY idx_shop_status_sort (shop_id, status, sort_no),
    KEY idx_shop_category (shop_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容文章';

ALTER TABLE `diy_material`
    ADD COLUMN recycled TINYINT NOT NULL DEFAULT 0 COMMENT '0文件库/1回收站' AFTER group_id,
    ADD COLUMN recycle_time DATETIME NULL COMMENT '移入回收站时间' AFTER recycled,
    ADD KEY idx_shop_recycled (shop_id, recycled, create_time);
