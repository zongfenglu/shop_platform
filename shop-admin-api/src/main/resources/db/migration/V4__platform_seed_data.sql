-- ============================================================
-- V4 平台默认数据：超级管理员账号 + 三档套餐模板
-- 密码为 BCrypt("admin123")，仅用于本地开发/演示环境，生产环境部署后必须立即修改。
-- 见文档一 §3.4 套餐配置示例、原型 admin/package-list.html 的三档套餐。
-- ============================================================

INSERT INTO platform_role (id, name, remark, is_builtin) VALUES
    (1, '超级管理员', '拥有全部权限，含平台设置与账号管理', 1);

-- BCrypt("admin123")，strength=10
INSERT INTO platform_user (id, username, password, real_name, status) VALUES
    (1, 'admin', '$2a$10$BisBrERipvZsQTOlTLDsm.JBoTz.qmgfr6ttQCP95073BeMS4JdGO', '超级管理员', 1);

INSERT INTO platform_user_role (id, user_id, role_id) VALUES
    (1, 1, 1);

INSERT INTO package_tpl (id, name, intro, menus, quota, price, is_trial, is_show, sort) VALUES
    (1, '基础版',
        '适合刚起步的小商家',
        JSON_ARRAY('goods.*', 'order.*', 'after_sale.*'),
        JSON_OBJECT('goods_max', 500, 'staff_max', 3, 'storage_mb', 5120, 'sms_month', 200, 'store_max', 0, 'diy_page_max', 5),
        JSON_OBJECT('month', 99, 'quarter', 269, 'year', 990),
        0, 1, 1),
    (2, '标准版',
        '营销中心全功能+二级分销',
        JSON_ARRAY('goods.*', 'order.*', 'after_sale.*', 'marketing.coupon', 'marketing.seckill', 'marketing.group', 'marketing.bargain', 'distribution.*', 'store.offline'),
        JSON_OBJECT('goods_max', 5000, 'staff_max', 10, 'storage_mb', 20480, 'sms_month', 1000, 'store_max', 5, 'diy_page_max', 30),
        JSON_OBJECT('month', 299, 'quarter', 799, 'year', 2680),
        0, 1, 2),
    (3, '旗舰版',
        '商品数不限+微信第三方托管+直播组件',
        JSON_ARRAY('goods.*', 'order.*', 'after_sale.*', 'marketing.*', 'distribution.*', 'store.offline', 'mp.authorize', 'live.*'),
        JSON_OBJECT('goods_max', -1, 'staff_max', 50, 'storage_mb', 102400, 'sms_month', 5000, 'store_max', -1, 'diy_page_max', -1),
        JSON_OBJECT('month', 699, 'quarter', 1899, 'year', 6800),
        0, 1, 3);

-- 试用套餐：新商城默认开通，权益等同基础版但配额更小、限时7天
INSERT INTO package_tpl (id, name, intro, menus, quota, price, is_trial, is_show, sort) VALUES
    (4, '试用版',
        '新商城默认试用7天，权益等同基础版',
        JSON_ARRAY('goods.*', 'order.*', 'after_sale.*'),
        JSON_OBJECT('goods_max', 100, 'staff_max', 2, 'storage_mb', 1024, 'sms_month', 50, 'store_max', 0, 'diy_page_max', 3),
        JSON_OBJECT('month', 0, 'quarter', 0, 'year', 0),
        1, 0, 99);
