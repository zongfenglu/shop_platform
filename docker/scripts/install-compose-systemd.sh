#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "请使用 root 运行此脚本" >&2
  exit 1
fi

DOCKER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${DOCKER_DIR}/docker-compose.yml"
DOCKER_BIN="$(command -v docker)"
UNIT_FILE="/etc/systemd/system/shop-platform-compose.service"

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "找不到 Compose 文件: ${COMPOSE_FILE}" >&2
  exit 1
fi

cat > "${UNIT_FILE}" <<EOF
[Unit]
Description=Shop Platform Docker Compose Stack
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=${DOCKER_DIR}
ExecStartPre=${DOCKER_BIN} compose -f ${COMPOSE_FILE} config --quiet
ExecStart=${DOCKER_BIN} compose -f ${COMPOSE_FILE} up -d --wait --wait-timeout 300
ExecReload=${DOCKER_BIN} compose -f ${COMPOSE_FILE} up -d --wait --wait-timeout 300
TimeoutStartSec=360

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now shop-platform-compose.service
systemctl --no-pager --full status shop-platform-compose.service
