-- ============================================================
-- V3 商户后台账号与权限（租户内 RBAC）
-- 见文档三 §3.6；文档二 §2.12 员工与权限
-- 这批表带 shop_id，走 ShopTenantLineHandler 的正常隔离路径
-- ============================================================

CREATE TABLE store_role (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(64)     NOT NULL COMMENT '角色名称，如：超级管理员/运营专员/客服专员/仓库专员/门店店员',
    menu_ids        JSON            NOT NULL COMMENT '菜单权限点ID集合',
    data_scope      VARCHAR(16)     NOT NULL DEFAULT 'all' COMMENT 'all全部数据 / store仅本门店（门店店员角色用）',
    is_builtin      TINYINT         NOT NULL DEFAULT 0 COMMENT '超级管理员角色不可编辑删除',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户角色（租户内RBAC）';

CREATE TABLE store_user (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NOT NULL,
    username        VARCHAR(64)     NOT NULL COMMENT '登录账号',
    password        VARCHAR(128)    NOT NULL COMMENT 'BCrypt 加密后的密码',
    real_name       VARCHAR(64)     NULL,
    mobile          VARCHAR(20)     NULL,
    role_id         BIGINT UNSIGNED NOT NULL,
    store_offline_id BIGINT UNSIGNED NULL COMMENT '门店店员角色时关联的门店ID，见 offline_store 表（M3阶段建表）',
    is_super_owner  TINYINT         NOT NULL DEFAULT 0 COMMENT '是否为超级店主（建店时自动创建，不可删除）',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
    last_login_time DATETIME        NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_username (shop_id, username),
    KEY idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户员工账号';
