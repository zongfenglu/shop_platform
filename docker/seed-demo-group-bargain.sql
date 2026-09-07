-- 本地演示种子数据（续）：拼团活动 + 砍价活动
-- 配合 seed-demo-goods.sql 的商品 8001(sku 9001,原价100) / 8003(sku 9003,原价40)
-- 可重复执行（先按 id 删除再插入）
-- 拼团：3人团，sku 9001 拼团价 79，开团后 24 小时成团，超时未成团由 GroupExpireJob 原路退款
-- 砍价：sku 9003 原价 40 砍到底价 9.9，最多 10 刀，发起后 48 小时有效

START TRANSACTION;

DELETE FROM group_active   WHERE id IN (2101);
DELETE FROM bargain_active  WHERE id IN (2201);

-- 1) 拼团活动：商品A sku 9001，3人团，拼团价 79（按 SKU 的 JSON 映射），24 小时成团，开启模拟成团
INSERT INTO group_active (id, shop_id, goods_id, group_num, group_price, valid_hours, is_mock, start_time, end_time, status)
VALUES (2101, 1001, 8001, 3, JSON_OBJECT('9001', 79.00), 24, 1,
        NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 'on');

-- 2) 砍价活动：商品C sku 9003，原价 40 砍到底价 9.9，最多 10 刀，48 小时有效
INSERT INTO bargain_active (id, shop_id, goods_id, floor_price, valid_hours, help_limit, start_time, end_time, status)
VALUES (2201, 1001, 8003, 9.90, 48, 10,
        NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 'on');

COMMIT;
