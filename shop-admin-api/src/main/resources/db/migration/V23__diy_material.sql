-- 商户素材库：记录商户上传过的图片，装修编辑器里可复用已上传的图，不必重复上传。
-- 物理文件由 LocalStorageServiceImpl 落在 shop.storage.local-dir（docker 下为共享卷 /data/uploads）。
-- 走正常租户隔离（带 shop_id，不加入 ShopTenantLineHandler 白名单）。
CREATE TABLE `diy_material` (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    url             VARCHAR(255)    NOT NULL COMMENT '相对URL，形如 /uploads/{shopId}/{yyyyMM}/{uuid}.jpg',
    name            VARCHAR(128)    NOT NULL COMMENT '原始文件名，仅用于列表展示',
    size            INT UNSIGNED    NOT NULL COMMENT '字节数',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户素材库';
