# 多开 SaaS 商城 — shop-platform

多商户独立部署体验的 SaaS 商城系统：一套系统、一次部署，为每个租户开出看起来完全独立的商城（独立小程序/独立域名/独立数据/独立品牌/独立收款账户）。

设计文档见 `docs/`：
- `01-产品设计-总体方案.md` — 产品定位、租户模型、架构关键决策、里程碑规划
- `02-产品设计-功能清单与流程.md` — 功能清单与关键业务流程
- `03-产品设计-数据模型与关键机制.md` — 数据模型、多租户实现、开发顺序建议
- `04-开发计划-Sprint排期.md` — Sprint 级排期（当前执行中）
- `05-部署运维手册.md` — 容器化部署、备份、排障
- `06-试点上线验收清单.md` — 年底接待真实商家的闸门
- `07-测试手册.md` — 给测试同学：账号、三个端、主路径、已知限制
- `prototype/` — 可点击高保真原型（前端开发直接对照复刻，不需要重新设计 UI）

## 工程结构

```
shop-platform/
├── shop-common/       通用：Result 封装、异常、错误码、枚举
├── shop-framework/    框架层：TenantContext、多租户拦截器、安全鉴权、缓存、越权防御测试脚手架
├── shop-domain/       领域层：按业务聚合分包（shop/goods/order/pay/...），domain 包间禁止反向依赖
├── shop-admin-api/    平台超管后台接口 /admin/**
├── shop-store-api/    商户后台接口 /store/**
├── shop-client-api/   消费者端接口 /api/**
├── shop-job/          定时任务（XXL-Job 执行器）
├── shop-mp/           微信第三方平台托管服务（M5 阶段实现）
├── shop-ui/
│   ├── admin/         超管后台前端（Vue3 + Ant Design Vue）
│   └── store/         商户后台前端（Vue3 + Ant Design Vue）
├── shop-uniapp/       消费者端（uni-app：小程序/H5/公众号/APP）
└── docker/            容器化部署
    ├── docker-compose.yml       本地开发依赖（MySQL/Redis/RocketMQ/XXL-Job）
    ├── docker-compose.prod.yml  生产全栈部署（前后端 + 依赖全进容器）
    ├── Dockerfile.backend       4 个后端应用共用（build arg MODULE 区分）
    ├── Dockerfile.frontend      3 个前端共用（build arg APP_DIR 区分）
    ├── nginx-{admin,store,h5}.conf
    └── .env.prod.example        生产环境变量模板
```

## 快速开始

### 后端

```bash
# 1. 启动本地依赖（MySQL/Redis/RocketMQ/XXL-Job）
docker compose -f docker/docker-compose.yml up -d

# 2. 编译 + 测试（含 ArchUnit 越权防御静态检查）
./mvnw compile
./mvnw test

# 3. 启动某个服务（以商户后台为例）
./mvnw -pl shop-store-api -am spring-boot:run
```

不需要本机安装 Maven —— 项目自带 Maven Wrapper（`./mvnw` / `mvnw.cmd`），首次运行会自动下载对应版本。

**本地开发只有依赖跑在 Docker 里**：后端应用（Spring Boot）和前端（Vite / uni-app dev server）都直接在宿主机上跑，`docker/docker-compose.yml` 只负责 MySQL/Redis/RocketMQ/XXL-Job。前后端一起进容器的**生产部署**见下方「生产部署」一节。

本地默认连接参数（`application.yml` 里的默认值已与 compose 对齐，零配置即可启动）：

| 依赖 | 宿主地址 | 账号/备注 |
| --- | --- | --- |
| MySQL | `localhost:3308` | `root` / `wchabc123!!`，库 `shop_platform`（3308 是为了避开本机其它 MySQL） |
| Redis | `localhost:6379` | 无密码；本机若已有别的项目占用 6379，先停掉再 `up -d` |
| RocketMQ NameServer | `localhost:9876` | 单机模式，仅本地开发 |
| XXL-Job Admin | http://localhost:8180/xxl-job-admin | 首次需导入 `xxl_job` 建表 SQL（见下） |

XXL-Job 调度中心第一次启动前要先导入建表 SQL，否则容器起来即报 SQL 错误：

```bash
curl -fsSL -o /tmp/tables_xxl_job.sql \
  https://raw.githubusercontent.com/xuxueli/xxl-job/2.4.2/doc/db/tables_xxl_job.sql
docker exec -i shop-platform-mysql mysql -uroot -p'wchabc123!!' < /tmp/tables_xxl_job.sql
docker restart shop-xxl-job-admin
```

> compose 文件里写了 `name: shop-platform`，**不要删掉**。不写的话 compose 会用所在目录名 `docker` 当项目名，而本机其它项目的 compose 也常放在各自的 `docker/` 目录下、服务名也叫 `mysql`/`redis` —— 项目名撞车会让 compose 把别的项目的容器当成本项目的旧容器删掉重建。

需要连别的库时用环境变量覆盖，不要改 yml：`DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` / `REDIS_HOST` / `REDIS_PORT` / `ROCKETMQ_NAME_SERVER`。

本地演示数据（商城 `demo` / 账号 `admin` / 密码 `123456`）用 `docker/seed/load.cmd`（Linux 用 `docker/seed/load.sh`）导入。必须带 `--default-character-set=utf8mb4`（脚本已写好）；在容器里 `mysql -e "source ..."` 会按 latin1 读 UTF-8 文件，中文会变成 `æ¼ç¤ºå•†åŸŽ` 这类乱码。

### 前端

```bash
cd shop-ui/store && npm install && npm run dev
cd shop-ui/admin && npm install && npm run dev
```

### 消费者端（uni-app）

```bash
cd shop-uniapp && npm install
npm run dev:mp-weixin   # 微信小程序
npm run dev:h5          # H5
```

## 生产部署（全栈容器）

与本地开发不同，生产用 `docker/docker-compose.prod.yml`：**4 个后端应用 + 3 个前端 + 全部依赖都在容器里**，镜像在容器内多阶段构建（宿主机不需要装 JDK/Maven/Node）。

```bash
# 1. 准备环境变量（所有 CHANGE_ME 必须改掉，否则 up 会直接失败）
cp docker/.env.prod.example docker/.env
vi docker/.env

# 2. 构建并启动后端 + 依赖
docker compose -f docker/docker-compose.prod.yml up -d --build

# 3. XXL-Job 首次初始化（导入调度中心建表 SQL 后重启）
curl -fsSL -o /tmp/tables_xxl_job.sql \
  https://raw.githubusercontent.com/xuxueli/xxl-job/2.4.2/doc/db/tables_xxl_job.sql
docker exec -i shop-prod-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /tmp/tables_xxl_job.sql
docker compose -f docker/docker-compose.prod.yml restart xxl-job-admin

# 4. 前端（需 shop-ui/* 与 shop-uniapp 已有源码，见下方说明）
docker compose -f docker/docker-compose.prod.yml --profile frontend up -d --build
```

| 服务 | 容器 | 默认宿主端口 | 说明 |
| --- | --- | --- | --- |
| admin-api | shop-prod-admin-api | 8081 | 平台超管接口；**唯一执行 Flyway 迁移的应用**，其余应用等它 healthy 后才启动 |
| store-api | shop-prod-store-api | 8082 | 商户后台接口 |
| client-api | shop-prod-client-api | 8083 | 消费者端接口（流量入口，堆和连接池给得最大） |
| job | shop-prod-job | 不暴露 | XXL-Job 执行器，仅容器网络内被调度中心回调 |
| admin-web / store-web / h5 | shop-prod-\* | 8090 / 8091 / 8092 | Nginx 静态托管 + 反代对应后端（`frontend` profile） |
| mysql / redis | shop-prod-\* | 不暴露 | 数据库端口默认不对外，需要直连排查时手动放开 compose 里注释的 `ports` |
| xxl-job-admin | shop-prod-xxl-job-admin | 8180 | 调度中心控制台 |

设计要点（改动前先看一眼，都是踩过的坑）：

- **前端服务默认不启动**。`shop-ui/admin`、`shop-ui/store`、`shop-uniapp` 目前还是空目录，没有 `package.json` 构建必然失败，所以三个 web 服务挂在 `frontend` profile 下。前端源码落地后直接加 `--profile frontend` 即可，Dockerfile 和 Nginx 配置已经写好。
- **密钥没有默认值**。`MYSQL_ROOT_PASSWORD` / `REDIS_PASSWORD` / `JWT_SECRET` / `AES_KEY` / `XXL_JOB_ACCESS_TOKEN` / `PLATFORM_BASE_DOMAIN` / `PAY_NOTIFY_BASE_URL` 在 compose 里写成 `${VAR:?...}`，缺失就中断启动——避免把 `application.yml` 里那些 `CHANGE_ME_IN_PRODUCTION_ENV_...` 开发默认值带上线。
- **启动顺序由 healthcheck 串起来**。`admin-api` 等 MySQL/Redis healthy，另外三个应用等 `admin-api` healthy（即 Flyway 迁移完成），否则会在表还没建好时启动、查询直接报表不存在。健康探针走 `/actuator/health`（为此四个应用都加了 actuator 依赖，`shop-job` 另外补了 web starter——它原本没有内嵌容器，`server.port: 8084` 其实不生效）。
- **actuator 只放开 `health`**。默认暴露的 `env`/`configprops` 会把数据库密码、JWT secret、商户支付凭证打在 HTTP 端点上；三份 Nginx 配置里也统一 `location /actuator/ { return 404; }`。
- **`SPRING_PROFILES_ACTIVE=docker`**：每个应用多了一份 `application-docker.yml`，把依赖地址切成 compose 服务名（`mysql` / `redis` / `rocketmq-namesrv`）、日志级别从 DEBUG 降到 INFO。
- **RocketMQ broker 的 `ROCKETMQ_BROKER_IP`**：broker 向 NameServer 注册的是自己的地址，客户端拿到后直连。全部客户端都在同一 compose 网络时用默认的服务名即可；只要有容器网络之外的客户端（宿主机进程、另一台机器），必须改成宿主机内网 IP，否则连接静默超时。
- **本地默认 `shop-app` 单进程**（超管/商户/C 端/任务/微信回调一个 JVM，堆 1g）。生产 compose 仍按模块拆开。RocketMQ 本地默认关闭，超时关单靠 5 分钟扫单。
- **JVM 用 `-XX:+UseContainerSupport -XX:MaxRAMPercentage=60`**，再叠加 `JAVA_OPTS` 的 `-Xmx`。拆分部署时每个后端 `mem_limit: 768m`、堆 512m。
- **日志按容器限流**（`max-size: 50m` × 5），不限制的话单容器日志能把磁盘写满。

镜像构建不跑测试（`-DskipTests`）：测试需要真实 MySQL/Redis，属于 CI 的职责——`.github/workflows/ci.yml` 里 `./mvnw verify` 已把 ArchUnit 与跨租户越权集成测试设为一票否决，镜像构建不重复。

## 开发前必读

- **越权防御是头号安全红线**：任何跨租户访问必须返回 403/404，相关静态检查（ArchUnit）与自动化测试是 CI 的一票否决项，见 `shop-framework/src/test/java/.../arch/NoBareGetByIdTest.java`。业务代码禁止裸用 `getById()`，必须走 `TenantSafeService#getByIdWithTenant`。
- **TenantContext 使用 TransmittableThreadLocal**，异步任务/线程池/MQ 消费场景必须自行确保上下文正确透传与清理，参考 `StoreTenantFilter` 的 try/finally 写法。
- 提交前先跑 `./mvnw test`，CI 会重复这一步，本地先过一遍能省一轮等待。

详见 `CONTRIBUTING.md`。
