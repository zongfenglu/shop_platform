-- 本地演示种子数据（续）：秒杀场次 / 秒杀活动 / 限时折扣活动 + 活动商品
-- 配合 demo-goods.sql 的商品 8001(sku 9001,原价100) / 8002(sku 9002,原价80)
-- 可重复执行（先按 id 删除再插入）
-- 注意：秒杀限量池在 Redis，DB 插入后需用 redis-cli 初始化（见 README/演示步骤），
--       或等对账 Job 每分钟自动 reconcile（stock = seckill_num - sold）。

START TRANSACTION;

DELETE FROM seckill_goods WHERE id IN (10001, 10002);
DELETE FROM seckill_active WHERE id IN (2001, 2002);
DELETE FROM seckill_time   WHERE id = 3001;

-- 1) 秒杀场次：10:00-23:59（演示时段内可抢）
INSERT INTO seckill_time (id, shop_id, name, start_time, end_time, sort, status)
VALUES (3001, 1001, '10:00场', '10:00:00', '23:59:59', 0, 'on');

-- 2) 秒杀活动：今日起 7 天，关联场次 3001
INSERT INTO seckill_active (id, shop_id, name, time_ids, start_date, end_date, status, remark)
VALUES (2001, 1001, '演示秒杀', JSON_ARRAY(3001), CURDATE(), DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'on', '演示用秒杀活动');

-- 3) 限时折扣活动：今日起 7 天，time_ids 为空（全天仅换价、无限购）
INSERT INTO seckill_active (id, shop_id, name, time_ids, start_date, end_date, status, remark)
VALUES (2002, 1001, '演示限时折扣', NULL, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'on', '演示用限时折扣');

-- 4) 秒杀商品：商品A sku 9001，原价100 → 秒杀价80，限量50，每人限购1
INSERT INTO seckill_goods (id, shop_id, active_id, goods_id, sku_id, seckill_price, seckill_num, limit_per_user, sold, status, sort)
VALUES (10001, 1001, 2001, 8001, 9001, 80.00, 50, 1, 0, 'on', 0);

-- 5) 限时折扣商品：商品B sku 9002，原价80 → 折扣价69，不限量、不限购
INSERT INTO seckill_goods (id, shop_id, active_id, goods_id, sku_id, seckill_price, seckill_num, limit_per_user, sold, status, sort)
VALUES (10002, 1001, 2002, 8002, 9002, 69.00, 0, 0, 0, 'on', 0);

COMMIT;
