# docker 目录

测试环境：改 `.env` 后 `docker compose up -d --build`。
宿主机 Nginx：把 `nginx/*.conf` 拷到 `/etc/nginx/conf.d/`（域名已写死 admin/store/h5.2doo.cn）。

| 路径 | 用途 |
|---|---|
| `nginx/` | **拷到宿主机** `/etc/nginx/conf.d/` |
| `frontend-nginx/` | Docker 前端容器内用，不要拷到宿主机 |
| `seed/` | 演示数据 |
