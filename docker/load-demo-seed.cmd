@echo off
REM Load local demo seeds with utf8mb4. Do NOT use `mysql -e "source ..."` —
REM the container client defaults to latin1 and will mojibake Chinese.
set MYSQL_PWD=wchabc123!!
set MYSQL=docker exec -i -e MYSQL_PWD=%MYSQL_PWD% shop-mysql mysql -uroot --default-character-set=utf8mb4 shop_platform
cd /d %~dp0
%MYSQL% < seed-demo.sql
%MYSQL% < seed-demo-goods.sql
%MYSQL% < seed-demo-seckill.sql
%MYSQL% < seed-demo-group-bargain.sql
%MYSQL% < seed-demo-offline.sql
echo Demo seed loaded (utf8mb4).
