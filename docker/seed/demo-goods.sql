-- 本地演示种子数据（续）：商品分类 / 运费模板 / 3 个单规格商品 + SKU
-- 配合 demo.sql 的商城 1001、券 4001(满100减10)、满减规则 5001(满99减10/满199减20,包邮)
-- 可重复执行（先按 id 删除再插入）

START TRANSACTION;

DELETE FROM goods_sku        WHERE id IN (9001, 9002, 9003);
DELETE FROM goods            WHERE id IN (8001, 8002, 8003);
DELETE FROM freight_template WHERE id = 6001;
DELETE FROM goods_category   WHERE id = 7001;

-- 1) 商品分类
INSERT INTO goods_category (id, shop_id, parent_id, name, sort, is_show)
VALUES (7001, 1001, 0, '演示分类', 0, 1);

-- 2) 运费模板：按件数计费，首件1件8元，每续1件加5元
INSERT INTO freight_template (id, shop_id, name, method, rules, free_rules)
VALUES (6001, 1001, '演示运费模板', 'count',
        JSON_ARRAY(JSON_OBJECT('region', JSON_ARRAY('*'), 'first', 1, 'firstFee', 8, 'additional', 1, 'additionalFee', 5)),
        NULL);

-- 3) 商品 A：单价 100，单规格
INSERT INTO goods (id, shop_id, category_ids, brand_id, name, sub_name, code, images, spec_type, content, status,
                   stock_total, delivery_type, freight_template_id, freight_fee, is_virtual, limit_type)
VALUES (8001, 1001, JSON_ARRAY(7001), NULL, '演示商品A', '单价100', 'DEMO-A',
        JSON_ARRAY('https://via.placeholder.com/300'), 'single', '<p>演示商品A</p>', 'on',
        100, JSON_ARRAY('express'), 6001, NULL, 0, 'none');
INSERT INTO goods_sku (id, shop_id, goods_id, sku_code, spec_value_ids, price, line_price, stock, weight, volume, image)
VALUES (9001, 1001, 8001, 'DEMO-A-1', '', 100.00, 120.00, 100, 0.5, 0.001, NULL);

-- 4) 商品 B：单价 80
INSERT INTO goods (id, shop_id, category_ids, brand_id, name, sub_name, code, images, spec_type, content, status,
                   stock_total, delivery_type, freight_template_id, freight_fee, is_virtual, limit_type)
VALUES (8002, 1001, JSON_ARRAY(7001), NULL, '演示商品B', '单价80', 'DEMO-B',
        JSON_ARRAY('https://via.placeholder.com/300'), 'single', '<p>演示商品B</p>', 'on',
        100, JSON_ARRAY('express'), 6001, NULL, 0, 'none');
INSERT INTO goods_sku (id, shop_id, goods_id, sku_code, spec_value_ids, price, line_price, stock, weight, volume, image)
VALUES (9002, 1001, 8002, 'DEMO-B-1', '', 80.00, 99.00, 100, 0.4, 0.001, NULL);

-- 5) 商品 C：单价 40（用来演示"未达满减门槛、产生运费"的场景）
INSERT INTO goods (id, shop_id, category_ids, brand_id, name, sub_name, code, images, spec_type, content, status,
                   stock_total, delivery_type, freight_template_id, freight_fee, is_virtual, limit_type)
VALUES (8003, 1001, JSON_ARRAY(7001), NULL, '演示商品C', '单价40', 'DEMO-C',
        JSON_ARRAY('https://via.placeholder.com/300'), 'single', '<p>演示商品C</p>', 'on',
        100, JSON_ARRAY('express'), 6001, NULL, 0, 'none');
INSERT INTO goods_sku (id, shop_id, goods_id, sku_code, spec_value_ids, price, line_price, stock, weight, volume, image)
VALUES (9003, 1001, 8003, 'DEMO-C-1', '', 40.00, 59.00, 100, 0.3, 0.001, NULL);

COMMIT;
