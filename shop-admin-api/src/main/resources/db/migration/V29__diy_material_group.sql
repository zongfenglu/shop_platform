-- 素材库分组：商户按业务（轮播/商品图/图标）归档图片，避免素材一锅粥。
-- group_id 为空表示未分组；删分组时把素材回落到未分组，不删物理文件。
CREATE TABLE `diy_material_group` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(32)     NOT NULL COMMENT '分组名称',
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户素材库分组';

ALTER TABLE `diy_material`
    ADD COLUMN `group_id` BIGINT UNSIGNED NULL COMMENT '所属分组，空=未分组' AFTER `size`,
    ADD KEY `idx_shop_group` (`shop_id`, `group_id`);
