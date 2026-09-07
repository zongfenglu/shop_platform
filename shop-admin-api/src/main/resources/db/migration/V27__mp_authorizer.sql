-- Sprint 17：微信开放平台第三方托管。
-- component_verify_ticket 由微信每 10 分钟推送到 shop-mp；
-- mp_authorizer 同时承载「自填 AppID」与「扫码授权托管」两种模式。
-- 代码模板只存微信侧 template_id，租户差异一律走 ext.json 注入，业务代码不允许有租户分支。

CREATE TABLE mp_component_ticket (
    id              TINYINT         NOT NULL PRIMARY KEY,
    ticket          VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '最近一次 component_verify_ticket，空表示尚未收到',
    received_time   DATETIME        NULL COMMENT '最近一次票据推送时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方平台 component_verify_ticket';

INSERT INTO mp_component_ticket (id, ticket, received_time, is_delete)
VALUES (1, '', NULL, 0);

CREATE TABLE mp_authorizer (
    id                      BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    shop_id                 BIGINT UNSIGNED NOT NULL,
    app_type                VARCHAR(16)     NOT NULL COMMENT 'mini小程序 / official公众号',
    auth_mode               VARCHAR(16)     NOT NULL COMMENT 'self自填 / hosted托管',
    appid                   VARCHAR(64)     NOT NULL,
    app_secret_encrypted    VARCHAR(1024)   NULL COMMENT '自填模式 AppSecret，AES-GCM',
    refresh_token_encrypted VARCHAR(1024)   NULL COMMENT '托管模式 authorizer_refresh_token，AES-GCM',
    func_info               TEXT            NULL COMMENT '授权权限集 JSON',
    auth_status             VARCHAR(16)     NOT NULL DEFAULT 'unauthorized' COMMENT 'unauthorized/authorized',
    nick_name               VARCHAR(128)    NULL,
    online_version          VARCHAR(64)     NULL,
    audit_status            VARCHAR(32)     NULL,
    authorized_time         DATETIME        NULL,
    unauthorized_time       DATETIME        NULL,
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete               TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_shop_app_type (shop_id, app_type),
    KEY idx_appid (appid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序/公众号授权（自填或第三方托管）';

CREATE TABLE mp_code_template (
    id              BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    template_id     VARCHAR(64)     NOT NULL COMMENT '微信代码模板 ID',
    user_version    VARCHAR(64)     NOT NULL,
    user_desc       VARCHAR(255)    NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'online' COMMENT 'online/disabled',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序代码模板库（ext.json 按租户注入）';
