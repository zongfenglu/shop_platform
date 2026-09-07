# docker 目录

试点（一台机、一个 JVM）：在本目录执行 `docker compose up -d --build`。  
生产拆分：`docker compose -f docker-compose.prod.yml up -d --build`（见 `docs/05-部署运维手册.md`）。

| 路径 | 用途 |
|---|---|
| `docker-compose.yml` | 试点全栈 |
| `docker-compose.prod.yml` | 生产：API 拆容器 |
| `Dockerfile.backend` / `Dockerfile.frontend` | 镜像构建 |
| `nginx/admin.conf` `store.conf` `h5.conf` | 前端容器内 Nginx；上游由构建参数 `BACKEND` 决定 |
| `nginx/host.conf` | **宿主机** Nginx，按域名转到 8090/8091/8092 |
| `seed/` | 演示数据，`./seed/load.sh` 或 `seed\load.cmd` |
| `scripts/` | 无 DNS 时写 hosts |
