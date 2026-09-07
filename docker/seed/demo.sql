-- 本地演示种子数据：一个商城 + 超管店主账号 + 两张示例券 + 一条满减规则
-- 仅用于本地开发联调，不进 Flyway（手动执行）；可重复执行（先按 id 删除再插入）

START TRANSACTION;

DELETE FROM full_reduce_rule WHERE id = 5001;
DELETE FROM coupon           WHERE id IN (4001, 4002);
DELETE FROM store_user       WHERE id = 3001;
DELETE FROM store_role       WHERE id = 2001;
DELETE FROM diy_tabbar       WHERE id = 12001 OR shop_id = 1001;
DELETE FROM shop_domain      WHERE id IN (13001, 13002) OR shop_id = 1001;
DELETE FROM shop_order       WHERE shop_id = 1001;
DELETE FROM shop_package     WHERE id = 14001 OR shop_id = 1001;
DELETE FROM shop             WHERE id = 1001;

-- 1) 商城（租户）+ 标准版套餐快照（package_tpl id=2，见 V4 种子）
INSERT INTO shop (id, code, name, industry, contact, mobile, status, expire_time, package_id)
VALUES (1001, 'demo', '演示商城', '美妆个护', '张老板', '13800000000', 'normal', DATE_ADD(NOW(), INTERVAL 365 DAY), 14001);

INSERT INTO shop_package (id, shop_id, package_tpl_id, name, menus, quota, price_snapshot, start_time, expire_time)
SELECT 14001, 1001, id, name, menus, quota, price, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY)
FROM package_tpl WHERE id = 2;

-- 2) 商户角色（超级管理员）
INSERT INTO store_role (id, shop_id, name, menu_ids, data_scope, is_builtin)
VALUES (2001, 1001, '超级管理员', JSON_ARRAY(), 'all', 1);

-- 3) 超管店主账号：admin / 123456（BCrypt）
INSERT INTO store_user (id, shop_id, username, password, real_name, role_id, is_super_owner, status)
VALUES (3001, 1001, 'admin',
        '$2a$10$egvxa3y99xRpID6VRAKBU.Z9ZSdhZl9KWDie8F0mhjyyf7JwPn0u.',
        '店主', 2001, 1, 1);

-- 4) 示例优惠券 1：满100减10（固定时间段）
INSERT INTO coupon (id, shop_id, name, type, reduce_price, min_price, expire_type, start_time, end_time, total_num, received_num, limit_per_user, apply_range, status)
VALUES (4001, 1001, '满100减10元', 'reduce', 10.00, 100.00, 'fixed', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 100, 0, 1, 'all', 'on');

-- 5) 示例优惠券 2：全场9折券（领取后7天有效）
INSERT INTO coupon (id, shop_id, name, type, discount_ratio, min_price, expire_type, expire_days, total_num, received_num, limit_per_user, apply_range, status)
VALUES (4002, 1001, '新人9折券', 'discount', 0.90, 50.00, 'receive', 7, 0, 0, 1, 'all', 'on');

-- 6) 示例满减规则：满99减10 / 满199减20，且满199包邮
INSERT INTO full_reduce_rule (id, shop_id, name, type, rules, free_express, status, sort)
VALUES (5001, 1001, '满199减20（包邮）', 'money',
        JSON_ARRAY(JSON_OBJECT('threshold', 99.00, 'reduce', 10.00), JSON_OBJECT('threshold', 199.00, 'reduce', 20.00)),
        1, 'on', 0);

-- 7) 底部导航：路径对齐 uni-app 真实路由（旧默认曾写成 /pages/cart/cart 等）
DELETE FROM diy_tabbar WHERE shop_id = 1001;
INSERT INTO diy_tabbar (id, shop_id, items)
VALUES (12001, 1001, JSON_ARRAY(
    JSON_OBJECT('icon', '', 'activeIcon', '', 'text', '首页', 'path', '/pages/index/index'),
    JSON_OBJECT('icon', '', 'activeIcon', '', 'text', '分类', 'path', '/pages/goods/list'),
    JSON_OBJECT('icon', '', 'activeIcon', '', 'text', '购物车', 'path', '/pages/cart/index'),
    JSON_OBJECT('icon', '', 'activeIcon', '', 'text', '我的', 'path', '/pages/my/index')
));

-- 8) 泛域名：demo.localhost / demo.shop.com，供 Host 识别（须 verify_status=verified）
INSERT INTO shop_domain (id, shop_id, domain, type, cert_status, verify_status, cname_status)
VALUES
    (13001, 1001, 'demo.localhost', 'sub', 'valid', 'verified', 'ok'),
    (13002, 1001, 'demo.shop.com', 'sub', 'valid', 'verified', 'ok');

COMMIT;
