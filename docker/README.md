# docker 目录

测试环境（`docker-compose.yml`）：只改 `.env` 的 `PLATFORM_BASE_DOMAIN`，然后：

```bash
cd docker
docker compose up -d --build
./seed/load.sh
sudo bash scripts/install-host-nginx.sh
```

| 路径 | 用途 |
|---|---|
| `docker-compose.yml` | 测试全栈，前端反代 `shop-app:8080` |
| `docker-compose.prod.yml` | 生产拆分 API |
| `nginx/admin.conf` `store.conf` `h5.conf` | 前端容器内 Nginx |
| `nginx/host.conf` | 宿主机 Nginx 模板（域名由安装脚本从 `.env` 填入） |
| `seed/` | 演示数据 |
| `scripts/install-host-nginx.sh` | 按 `.env` 域名启用宿主机 Nginx |
