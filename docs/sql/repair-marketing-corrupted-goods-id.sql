-- =====================================================================
-- 修复：营销活动表中被 JS Number() 取整写坏的 19 位雪花商品/SKU id
-- =====================================================================
-- 背景：2026-09-09 前的商户端前端在提交秒杀/拼团/砍价商品时对 id 做了
-- Number() 转换，超过 JS Number.MAX_SAFE_INTEGER（约 16 位）的 19 位雪花
-- id 被静默取整成另一个"看起来合法但不存在"的 id（典型特征：结尾一串 0，
-- 如 2097137709666820000）。后端按租户查不到该商品，报"无权限访问：id=..."。
--
-- 取整误差有界：id < 2^62 时，双精度浮点相邻可表示值间距 ≤ 512，
-- 即 |坏id - 真id| ≤ 256。下面用 ±512 窗口 + 同 shop_id 匹配唯一真实商品，
-- 雪花 id 含毫秒时间戳，同店铺两个商品 id 相差 <512（同一毫秒生成）实际不可能，
-- 误匹配风险可忽略；保险起见先跑"检测"确认每条坏记录只匹配到一个候选。
--
-- 用法：先逐条跑【检测】部分确认匹配结果，再执行【修复】部分。
-- =====================================================================

-- ---------- 检测：找出指向不存在商品的营销记录，并给出候选真实 id ----------

-- 秒杀商品（goods_id + sku_id 都可能坏）
SELECT 'seckill_goods' AS tbl, sg.id, sg.shop_id,
       sg.goods_id AS bad_goods_id, g2.id AS real_goods_id, g2.name,
       sg.sku_id  AS bad_sku_id,  k2.id AS real_sku_id
FROM seckill_goods sg
LEFT JOIN goods g  ON g.id = sg.goods_id AND g.is_delete = 0
LEFT JOIN goods g2 ON g2.shop_id = sg.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN sg.goods_id - 512 AND sg.goods_id + 512
LEFT JOIN goods_sku k2 ON k2.goods_id = g2.id
       AND k2.id BETWEEN sg.sku_id - 512 AND sg.sku_id + 512
WHERE sg.is_delete = 0 AND g.id IS NULL;

-- 拼团活动（goods_id；group_price 的 JSON key 是 sku_id，见文末说明）
SELECT 'group_active' AS tbl, a.id, a.shop_id,
       a.goods_id AS bad_goods_id, g2.id AS real_goods_id, g2.name
FROM group_active a
LEFT JOIN goods g  ON g.id = a.goods_id AND g.is_delete = 0
LEFT JOIN goods g2 ON g2.shop_id = a.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN a.goods_id - 512 AND a.goods_id + 512
WHERE a.is_delete = 0 AND g.id IS NULL;

-- 砍价活动（goods_id）
SELECT 'bargain_active' AS tbl, a.id, a.shop_id,
       a.goods_id AS bad_goods_id, g2.id AS real_goods_id, g2.name
FROM bargain_active a
LEFT JOIN goods g  ON g.id = a.goods_id AND g.is_delete = 0
LEFT JOIN goods g2 ON g2.shop_id = a.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN a.goods_id - 512 AND a.goods_id + 512
WHERE a.is_delete = 0 AND g.id IS NULL;

-- 积分商城兑换项（goods_id / coupon_id 二选一）
SELECT 'points_goods' AS tbl, p.id, p.shop_id,
       p.goods_id AS bad_goods_id, g2.id AS real_goods_id, g2.name
FROM points_goods p
LEFT JOIN goods g  ON g.id = p.goods_id AND g.is_delete = 0
LEFT JOIN goods g2 ON g2.shop_id = p.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN p.goods_id - 512 AND p.goods_id + 512
WHERE p.is_delete = 0 AND p.goods_id IS NOT NULL AND g.id IS NULL;

-- 优惠券适用范围（apply_range_config 是 JSON 数组文本，只能人工核对）
SELECT 'coupon' AS tbl, id, shop_id, apply_range, apply_range_config
FROM coupon
WHERE is_delete = 0 AND apply_range = 'goods'
  AND apply_range_config LIKE '%000]%' ;  -- 结尾多个 0 的 id 高度可疑，人工核对

-- ---------- 修复：确认上面每条坏记录 real_goods_id 唯一后执行 ----------

-- 秒杀有 uk_active_sku(shop_id, active_id, sku_id) 唯一键。商户发现"添加失败"
-- （实际写入了坏 id）后往往会重新添加一次成功的，坏行修复后会与好行冲突。
-- 所以分两步：1) 修复后会重复的坏行 → 软删除；2) 其余坏行 → 修复。

-- 1) 坏行修复后与现存行重复：软删除（保留坏 sku_id，不会占用唯一键）
UPDATE seckill_goods sg
JOIN goods g2 ON g2.shop_id = sg.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN sg.goods_id - 512 AND sg.goods_id + 512
LEFT JOIN goods g ON g.id = sg.goods_id AND g.is_delete = 0
JOIN goods_sku k2 ON k2.goods_id = g2.id
       AND k2.id BETWEEN sg.sku_id - 512 AND sg.sku_id + 512
JOIN seckill_goods dup ON dup.shop_id = sg.shop_id AND dup.active_id = sg.active_id
       AND dup.sku_id = k2.id AND dup.is_delete = 0 AND dup.id <> sg.id
SET sg.is_delete = 1
WHERE sg.is_delete = 0 AND g.id IS NULL;

-- 2) 无冲突的坏行：修复 goods_id + sku_id
UPDATE seckill_goods sg
JOIN goods g2 ON g2.shop_id = sg.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN sg.goods_id - 512 AND sg.goods_id + 512
LEFT JOIN goods g ON g.id = sg.goods_id AND g.is_delete = 0
LEFT JOIN goods_sku k2 ON k2.goods_id = g2.id
       AND k2.id BETWEEN sg.sku_id - 512 AND sg.sku_id + 512
LEFT JOIN seckill_goods dup ON dup.shop_id = sg.shop_id AND dup.active_id = sg.active_id
       AND dup.sku_id = k2.id AND dup.is_delete = 0 AND dup.id <> sg.id
SET sg.goods_id = g2.id,
    sg.sku_id   = COALESCE(k2.id, sg.sku_id)
WHERE sg.is_delete = 0 AND g.id IS NULL AND dup.id IS NULL;

UPDATE group_active a
JOIN goods g2 ON g2.shop_id = a.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN a.goods_id - 512 AND a.goods_id + 512
LEFT JOIN goods g ON g.id = a.goods_id AND g.is_delete = 0
SET a.goods_id = g2.id
WHERE a.is_delete = 0 AND g.id IS NULL;

UPDATE bargain_active a
JOIN goods g2 ON g2.shop_id = a.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN a.goods_id - 512 AND a.goods_id + 512
LEFT JOIN goods g ON g.id = a.goods_id AND g.is_delete = 0
SET a.goods_id = g2.id
WHERE a.is_delete = 0 AND g.id IS NULL;

UPDATE points_goods p
JOIN goods g2 ON g2.shop_id = p.shop_id AND g2.is_delete = 0
       AND g2.id BETWEEN p.goods_id - 512 AND p.goods_id + 512
LEFT JOIN goods g ON g.id = p.goods_id AND g.is_delete = 0
SET p.goods_id = g2.id
WHERE p.is_delete = 0 AND p.goods_id IS NOT NULL AND g.id IS NULL;

-- ---------- 修复后复查：四张表应查不出任何悬空 goods_id ----------
SELECT 'seckill_goods' tbl, COUNT(*) dangling FROM seckill_goods sg
  LEFT JOIN goods g ON g.id = sg.goods_id AND g.is_delete = 0
  WHERE sg.is_delete = 0 AND g.id IS NULL
UNION ALL
SELECT 'group_active', COUNT(*) FROM group_active a
  LEFT JOIN goods g ON g.id = a.goods_id AND g.is_delete = 0
  WHERE a.is_delete = 0 AND g.id IS NULL
UNION ALL
SELECT 'bargain_active', COUNT(*) FROM bargain_active a
  LEFT JOIN goods g ON g.id = a.goods_id AND g.is_delete = 0
  WHERE a.is_delete = 0 AND g.id IS NULL
UNION ALL
SELECT 'points_goods', COUNT(*) FROM points_goods p
  LEFT JOIN goods g ON g.id = p.goods_id AND g.is_delete = 0
  WHERE p.is_delete = 0 AND p.goods_id IS NOT NULL AND g.id IS NULL;

-- =====================================================================
-- 附注：
-- 1. group_active.group_price 的 JSON key 是 sku_id，旧前端同样可能把 key
--    写坏。SQL 批量修 JSON key 风险大，数量少的话建议在商户端"编辑拼团"里
--    重新填一遍拼团价保存（新前端已按字符串直传，保存即修复）。
-- 2. 修复前请先备份：mysqldump 上述四张表。
-- 3. 必须先部署 2026-09-09 及之后构建的 store-web 前端再让商户继续操作，
--    否则旧前端会继续写入坏 id。
-- =====================================================================
