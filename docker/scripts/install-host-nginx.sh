#!/usr/bin/env bash
# 读取 docker/.env 的 PLATFORM_BASE_DOMAIN，安装/启用宿主机 Nginx。
# 用法：sudo bash docker/scripts/install-host-nginx.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT/.env"
TEMPLATE="$ROOT/nginx/host.conf"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "请用 sudo 运行" >&2
  exit 1
fi
if [[ ! -f "$ENV_FILE" ]]; then
  echo "缺少 $ENV_FILE" >&2
  exit 1
fi
domain=$(grep -E '^\s*PLATFORM_BASE_DOMAIN\s*=' "$ENV_FILE" | tail -n 1 \
  | sed -E 's/^\s*PLATFORM_BASE_DOMAIN\s*=\s*//' | tr -d ' "')
if [[ -z "$domain" ]]; then
  echo "docker/.env 里没有 PLATFORM_BASE_DOMAIN" >&2
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

# 清掉误拷到宿主机的容器配置（里面有 Docker 服务名 app，宿主机解析不了）
rm -f /etc/nginx/conf.d/admin.conf /etc/nginx/conf.d/store.conf /etc/nginx/conf.d/h5.conf
rm -f /etc/nginx/sites-enabled/admin.conf /etc/nginx/sites-enabled/store.conf /etc/nginx/sites-enabled/h5.conf

tmp=$(mktemp)
sed "s/__DOMAIN__/${domain}/g" "$TEMPLATE" > "$tmp"

if [[ -d /etc/nginx/sites-available ]]; then
  dest=/etc/nginx/sites-available/shop-platform
  cp "$tmp" "$dest"
  ln -sf "$dest" /etc/nginx/sites-enabled/shop-platform
  rm -f /etc/nginx/sites-enabled/default
else
  dest=/etc/nginx/conf.d/shop-platform.conf
  cp "$tmp" "$dest"
fi
rm -f "$tmp"

nginx -t
systemctl enable --now nginx
systemctl reload nginx
echo "已启用 http://admin.${domain}  http://store.${domain}  http://demo.${domain}"
