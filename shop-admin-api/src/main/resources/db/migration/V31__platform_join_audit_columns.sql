-- 关联表建表时漏了审计列，实体继承 BaseEntity（含 create_time / update_time / is_delete + 逻辑删除）。
-- 打开平台账号、给用户绑角色时会 SELECT 这些列，MySQL 报 Unknown column 'create_time'。
-- 幂等：列已存在则跳过（上次迁移可能只加了一张表就失败）。

SET @db = DATABASE();

SET @exist = (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'platform_user_role' AND COLUMN_NAME = 'create_time');
SET @sql = IF(@exist = 0,
    'ALTER TABLE platform_user_role ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, ADD COLUMN is_delete TINYINT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist = (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'platform_role_menu' AND COLUMN_NAME = 'create_time');
SET @sql = IF(@exist = 0,
    'ALTER TABLE platform_role_menu ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, ADD COLUMN is_delete TINYINT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
