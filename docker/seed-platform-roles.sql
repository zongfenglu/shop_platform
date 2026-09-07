INSERT INTO platform_role (id, name, remark, is_builtin) VALUES
  (2, '运营专员', '商城管理、套餐管理、订购管理', 1),
  (3, '客服专员', '商城查看、免密登录代客服务', 1),
  (4, '财务专员', '订购管理、退款审核、发票管理', 1);

INSERT INTO platform_menu (id, parent_id, name, code, type, path, icon, sort) VALUES
(1, 0, '数据看板', 'dashboard', 2, '/dashboard', '◨', 1),
(2, 0, '商城管理', 'shop', 1, '/shops', '▤', 2),
(3, 2, '查看商城列表', 'shop:list', 3, NULL, NULL, 1),
(4, 2, '新建商城', 'shop:create', 3, NULL, NULL, 2),
(5, 2, '查看商城详情', 'shop:detail', 3, NULL, NULL, 3),
(6, 0, '套餐管理', 'package', 1, '/packages', '◩', 3),
(7, 6, '查看套餐', 'package:list', 3, NULL, NULL, 1),
(8, 6, '新建/编辑套餐', 'package:write', 3, NULL, NULL, 2),
(9, 0, '订购管理', 'order', 1, '/orders', '￥', 4),
(10, 9, '查看订购', 'order:list', 3, NULL, NULL, 1),
(11, 9, '审核/退款', 'order:review', 3, NULL, NULL, 2),
(12, 0, '域名管理', 'domain', 1, '/domains', '⌘', 5),
(13, 0, '客户端管理', 'client', 1, '/clients', '▣', 6),
(20, 0, '平台账号', 'account', 1, '/accounts', '◎', 10),
(21, 20, '查看管理员', 'account:list', 3, NULL, NULL, 1),
(22, 20, '新建/编辑管理员', 'account:write', 3, NULL, NULL, 2),
(23, 0, '平台设置', 'setting', 1, '/settings', '⚙', 11),
(24, 23, '查看设置', 'setting:list', 3, NULL, NULL, 1),
(25, 23, '修改设置', 'setting:write', 3, NULL, NULL, 2),
(26, 0, '运维中心', 'ops', 1, '/ops', '⏱', 12);

-- 超管拥有全部
INSERT INTO platform_role_menu (id, role_id, menu_id)
SELECT id + 100, 1, id FROM platform_menu;

-- 运营专员: 数据看板 + 商城管理 + 套餐管理 + 订购管理 + 域名管理
INSERT INTO platform_role_menu (id, role_id, menu_id) VALUES
(200, 2, 1),(201, 2, 2),(202, 2, 3),(203, 2, 4),(204, 2, 5),
(205, 2, 6),(206, 2, 7),(207, 2, 8),(208, 2, 9),(209, 2, 10),
(210, 2, 11),(211, 2, 12);

-- 客服专员: 数据看板 + 商城列表查看 + 订购查看 + 平台账号查看
INSERT INTO platform_role_menu (id, role_id, menu_id) VALUES
(300, 3, 1),(301, 3, 2),(302, 3, 3),(303, 3, 5),
(304, 3, 9),(305, 3, 10),(306, 3, 20),(307, 3, 21);

-- 财务专员: 订购管理全部 + 套餐查看
INSERT INTO platform_role_menu (id, role_id, menu_id) VALUES
(400, 4, 9),(401, 4, 10),(402, 4, 11),(403, 4, 6),(404, 4, 7);
