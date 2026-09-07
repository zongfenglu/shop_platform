-- Sprint 15 追加范围: 商户私有装修模板（"存为模板"功能）。
-- 与 diy_template（平台级行业模板，无 shop_id，只读）语义分离：
-- diy_my_template 走正常租户隔离（非白名单表），每个商户维护自己的私有模板。

CREATE TABLE `diy_my_template` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(32)     NOT NULL,
    page_data       JSON            NOT NULL COMMENT '结构与 diy_page.page_data 一致',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户私有装修模板';
