#!/usr/bin/env bash
# Load local demo seeds with utf8mb4. Do not use mysql -e "source ..." —
# the container client defaults to latin1 and will mojibake Chinese.
set -euo pipefail
cd "$(dirname "$0")"
MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-wchabc123!!}"
run() {
  docker exec -i -e MYSQL_PWD="$MYSQL_PWD" shop-mysql \
    mysql -uroot --default-character-set=utf8mb4 shop_platform < "$1"
}
run demo.sql
run demo-goods.sql
run demo-seckill.sql
run demo-group-bargain.sql
run demo-offline.sql
echo "Demo seed loaded (utf8mb4)."
