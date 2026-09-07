-- 本地演示种子数据（续）：自提门店
-- 配合 seed-demo.sql 的商城 1001。可重复执行。

START TRANSACTION;

DELETE FROM offline_store WHERE id IN (11001, 11002);

INSERT INTO offline_store (id, shop_id, name, phone, region, detail, longitude, latitude, business_hours, status)
VALUES
    (11001, 1001, '演示商城(西湖银泰店)', '0571-88880001',
     '浙江省杭州市西湖区', '龙翔路1号银泰百货3F',
     120.130061, 30.259244, '10:00-22:00', 'enabled'),
    (11002, 1001, '演示商城(万象城店)', '0571-88880002',
     '浙江省杭州市上城区', '新塘路1号万象城1F',
     120.210012, 30.257801, '10:00-22:00', 'enabled');

COMMIT;
