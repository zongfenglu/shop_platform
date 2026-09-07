#!/usr/bin/env bash
# 把 docker/nginx/*.conf 拷到 /etc/nginx/conf.d（域名已写死为 2doo.cn）。
# 用法：sudo bash docker/scripts/install-host-nginx.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/nginx"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "请用 sudo 运行" >&2
  exit 1
fi

if ! command -v nginx >/dev/null 2>&1; then
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update && apt-get install -y nginx
  elif command -v yum >/dev/null 2>&1; then
    yum install -y nginx
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y nginx
  else
    echo "请先安装 nginx" >&2
    exit 1
  fi
fi

rm -f /etc/nginx/conf.d/shop-platform.conf /etc/nginx/conf.d/host.conf
rm -f /etc/nginx/sites-enabled/default /etc/nginx/sites-enabled/shop-platform
if [[ -f /etc/nginx/conf.d/default.conf ]]; then
  mv -f /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak
fi
cp "$SRC/admin.conf" "$SRC/store.conf" "$SRC/h5.conf" /etc/nginx/conf.d/

nginx -t
systemctl enable --now nginx
systemctl reload nginx
echo "已启用 http://admin.2doo.cn  http://store.2doo.cn  http://h5.2doo.cn"
