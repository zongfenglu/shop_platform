-- ============================================================
-- V1 平台账号与权限（RBAC）
-- 见文档三 §3.1 平台层；文档一 §2 角色与使用场景
-- 这批表不带 shop_id，属于 ShopTenantLineHandler 的忽略表白名单
-- ============================================================

CREATE TABLE platform_user (
    id              BIGINT UNSIGNED PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL COMMENT '登录账号',
    password        VARCHAR(128)    NOT NULL COMMENT 'BCrypt 加密后的密码',
    real_name       VARCHAR(64)     NULL COMMENT '姓名',
    mobile          VARCHAR(20)     NULL,
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
    last_login_time DATETIME        NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台管理员账号';

CREATE TABLE platform_role (
    id              BIGINT UNSIGNED PRIMARY KEY,
    name            VARCHAR(64)     NOT NULL COMMENT '角色名称，如：超级管理员/运营专员/客服专员/财务专员',
    remark          VARCHAR(255)    NULL,
    is_builtin      TINYINT         NOT NULL DEFAULT 0 COMMENT '内置角色不可编辑删除（如超级管理员）',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台角色';

CREATE TABLE platform_menu (
    id              BIGINT UNSIGNED PRIMARY KEY,
    parent_id       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    name            VARCHAR(64)     NOT NULL COMMENT '菜单名称',
    code            VARCHAR(64)     NOT NULL COMMENT '权限码，如 shop:list / shop:create',
    type            TINYINT         NOT NULL DEFAULT 1 COMMENT '1目录 2菜单 3按钮',
    path            VARCHAR(128)    NULL COMMENT '前端路由路径',
    icon            VARCHAR(64)     NULL,
    sort            INT             NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台菜单与权限点';

CREATE TABLE platform_role_menu (
    id              BIGINT UNSIGNED PRIMARY KEY,
    role_id         BIGINT UNSIGNED NOT NULL,
    menu_id         BIGINT UNSIGNED NOT NULL,
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联';

CREATE TABLE platform_user_role (
    id              BIGINT UNSIGNED PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    role_id         BIGINT UNSIGNED NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

CREATE TABLE platform_setting (
    id              BIGINT UNSIGNED PRIMARY KEY,
    setting_key     VARCHAR(64)     NOT NULL COMMENT '配置项key，如 storage/sms/express/wechat/pay/site',
    setting_value   JSON            NOT NULL COMMENT '配置内容，敏感字段（AK/SK/密钥）加密存储后再落入json',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台设置（存储/短信/物流/微信/支付/站点）';

CREATE TABLE sys_log (
    id              BIGINT UNSIGNED PRIMARY KEY,
    shop_id         BIGINT UNSIGNED NULL COMMENT '关联商城，平台自身操作可为空',
    operator_type   TINYINT         NOT NULL COMMENT '1平台管理员 2商户员工',
    operator_id     BIGINT UNSIGNED NOT NULL,
    operator_name   VARCHAR(64)     NULL,
    by_platform     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否为平台代管理操作（免密登录场景），见文档一§1.2',
    action          VARCHAR(128)    NOT NULL COMMENT '操作类型，如：免密登录/套餐续费/域名审核',
    description     VARCHAR(512)    NULL,
    ip              VARCHAR(64)     NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_shop_time (shop_id, create_time),
    KEY idx_operator (operator_type, operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台+商户操作审计日志';
