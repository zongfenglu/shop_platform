# 多开 SaaS 商城 — 开发计划（四）Sprint 排期

> 配套文档：`01-产品设计-总体方案.md`（里程碑总纲 §6.1）、`02-产品设计-功能清单与流程.md`（功能优先级）、`03-产品设计-数据模型与关键机制.md`（表设计与开发顺序建议 §11）
> 团队规模：**3~5 人**（1~2 后端 + 1 前端 + 1 客户端/全栈）
> 迭代节奏：**2 周一个 Sprint**
> 前端页面已有可点击高保真原型（`docs/prototype/`），前端开发**直接对照原型复刻交互与状态，不需要重新设计 UI** —— 本计划里凡是提到具体页面，都标注了对应原型文件路径。

---

## 0. 总览时间线

| Sprint | 周期 | 里程碑 | 目标 |
|---|---|---|---|
| S0 | 第1周 | 启动前置 | 工程基座、CI/CD、规范落地 |
| S1~S2 | 第2~5周 | **M0 基座** | 能开店、能登录 |
| S3~S6 | 第6~13周 | **M1 交易闭环** | 能卖出第一单 → 小范围试点交付 |
| S7~S10 | 第14~21周 | **M2 会员与营销** | 能做活动 |
| S11~S13 | 第22~27周 | **M3 分销与门店** | 能裂变 |
| S14~S16 | 第28~33周 | **M4 装修与多端** | 能像自己的店 |
| S17~S19 | 第34~39周 | **M5 SaaS 运营** | 能规模化交付 → 正式对外招商 |

全程约 **39 周（~9 个月）**，5 人团队按此排期；若实际只有 3 人，见文末 §11 降级方案（周期拉长至 1.4~1.6 倍）。

---

## S0：启动前置（第1周）

**目标**：工程基座就位，第一天写的代码就在正确的架子上。

| 任务 | 负责人 | 产出 |
|---|---|---|
| 仓库结构初始化（`shop-common/framework/domain/admin-api/store-api/client-api/job/mp` + `shop-ui/{admin,store}` + `shop-uniapp`） | BE负责人 | 对应文档三 §1 工程结构 |
| CI/CD：Maven构建 + 单测 + Flyway migration 校验 | BE负责人 | Pipeline 绿灯 |
| ArchUnit 规则：禁止裸用 `getById`、强制走 `getByIdWithTenant` | BE负责人 | 静态检查规则文件 |
| 本地基础设施：MySQL/Redis/RocketMQ/XXL-Job docker-compose | BE | 一键启动本地环境 |
| Git 分支策略 + PR 模板 + Code Review checklist | 全员 | CONTRIBUTING.md |
| 前端工程初始化：Vue3 + Ant Design Vue（admin/store 双套） | FE | 空壁架子可跑通登录页 |
| uni-app 工程初始化 + 条件编译配置（小程序/H5/公众号/APP） | Client | 空壁架子可预览 |

**验收**：`git clone` → 一条命令拉起本地全套依赖 → 前后端都能跑通一个 Hello World 请求。

---

## M0 基座（S1~S2，4周）

**目标**：能开店、能登录。**这个阶段的两件事补做成本最高，必须第一批做完**：越权防御测试进CI、租户上下文机制。

### Sprint 1（第2~3周）

| 模块 | 任务 | 对应文档 |
|---|---|---|
| BE-核心 | `TenantContext`（TransmittableThreadLocal） + MyBatis-Plus 租户拦截器 | 文档三 §2.1~2.3 |
| BE-核心 | ★越权防御：写操作二次校验 + ArchUnit 静态检查 + **自动化越权测试脚手架接入 CI（一票否决）** | 文档三 §2.4 |
| BE-核心 | `shop`/`shop_package`/`package_tpl` 表 + Flyway migration | 文档三 §3.1 |
| BE-核心 | 平台 RBAC：`platform_user`/`platform_role`/`platform_menu` | 文档三 §3.1 |
| FE-admin | 超管登录页 + 后台外壳（侧边栏/topbar） | 原型 `admin/login.html` |

**验收**：越权测试用例覆盖首批接口并接入 CI gate，任一接口跨租户访问必须返回 403/404；平台管理员可登录看到菜单骨架。

### Sprint 2（第4~5周）

| 模块 | 任务 | 对应文档 |
|---|---|---|
| BE-核心 | 建店流程：种子数据灌入（默认分类/运费模板/装修页/交易设置/会员等级/协议），**要求幂等可重试、<5秒** | 文档二 §1.2 |
| BE-核心 | 套餐管理 API（CRUD + **快照机制**：套餐模板改动不影响已开通商城） | 文档一 §3.4，文档二 §1.3 |
| BE-核心 | 免密登录 token（一次性、5分钟有效、单次使用）+ 操作审计日志（`by_platform` 标记） | 文档二 §1.2 |
| BE-核心 | `store_user`/`store_role` 表 + 商户后台 JWT 鉴权（带 shopId） | 文档三 §3.6 |
| FE-admin | 商城列表、新建商城向导（5步）、商城详情 | 原型 `admin/shop-list.html`、`shop-new.html`、`shop-detail.html` |
| FE-admin | 套餐管理页面（功能项树形勾选 + 配额配置） | 原型 `admin/package-list.html` |
| FE-store | 商户后台登录 + 首页看板骨架 | 原型 `store/login.html`、`dashboard.html` |

**验收（M0 出口标准）**：超管后台建一个新商城 → 短信下发账号 → 店主登录商户后台 → 看到的菜单是**套餐权限 ∩ 角色权限**的交集；免密登录能进商户后台且显示"平台代管理中"横幅。

---

## M1 交易闭环（S3~S6，8周）

**目标**：能卖出第一单。**价格计算引擎和越权测试是这个阶段风险最高的两件事**，价格引擎哪怕本阶段只用到 2 个 Handler，也要按完整责任链的骨架搭，否则 M2 接营销玩法时要推翻重做。

### Sprint 3（第6~7周）— 商品域

| 模块 | 任务 |
|---|---|
| BE | `goods`/`goods_sku`/`goods_category`/`goods_spec`/`goods_spec_value` 表 + 单/多规格发布逻辑（单规格也生成一条 SKU，下单只走一套代码路径） |
| BE | 商品 CRUD API、批量导入导出、OSS/本地存储适配 |
| FE-store | 商品列表、发布/编辑页（单多规格切换 + SKU矩阵） | 原型 `store/goods-list.html`、`goods-edit.html` |
| Client | 首页（静态布局，装修引擎在M4才接）、分类页、商品详情、购物车骨架 | 原型 `h5/home.html`、`goods-detail.html` |

**验收**：发布一个3色3码的多规格商品，uniapp端商品详情页规格选择器正确联动库存与价格。

### Sprint 4（第8~9周）— 价格引擎 + 下单

| 模块 | 任务 |
|---|---|
| BE | ★**价格计算引擎骨架**：责任链模式，先落地 `BasePriceHandler` → `FreightHandler` → `RoundingHandler`，为 M2 的4个营销 Handler 预留插槽 | 文档三 §4 |
| BE | `order`/`order_goods`/`order_address` 表 + 下单 API（**下单减库存**，乐观锁扣减） |
| BE | 购物车 API、`freight_template` 运费模板 |
| FE-store | 订单列表/详情骨架 |
| Client | 购物车页、确认订单页、地址管理 | 原型 `h5/cart.html`、`checkout.html` |

**验收**：价格引擎单测覆盖"多项优惠叠加并分摊到每个订单行，分摊金额总和=实付金额"的场景（即使当前只有运费一项）；下单库存扣减并发压测无超卖。

### Sprint 5（第10~11周）— 支付 + 订单流程

| 模块 | 任务 |
|---|---|
| BE | 微信支付接入（**租户自有商户号**）+ 支付回调幂等处理 + 主动查询补偿定时任务 |
| BE | 订单状态机：待付款→待发货→待收货→已完成/已取消 |
| BE | 发货 API（批量发货、部分发货多包裹） |
| BE | 支付密钥 AES-256 加密存储（前端只显示掩码） | 文档三 §9 |
| BE | RocketMQ 超时关单延时消息 |
| FE-store | 发货弹窗、订单状态操作、支付设置页 | 原型 `store/order-detail.html`、`settings.html` |
| Client | 收银台/支付结果页、我的订单列表/详情 | 原型 `h5/pay-result.html`、`order-list.html` |

**验收**：完整走通"下单→支付→发货→确认收货"；支付回调重复投递不重复处理订单（幂等校验用商户订单号+状态机乐观锁）。

### Sprint 6（第12~13周）— 售后 + 物流 + M1 收尾

| 模块 | 任务 |
|---|---|
| BE | `after_sale` 售后单状态机（仅退款/退货退款），**退款按优惠分摊比例逆向计算** |
| BE | 物流公司查询接入（快递100/快递鸟） |
| BE | 库存回滚逻辑 |
| FE-store | 售后管理列表+审核弹窗 | 原型 `store/after-sale-list.html` |
| Client | 申请售后流程、物流轨迹查看、商品评价页 | 原型 `h5/goods-comment.html` |

**M1 质量门禁（收尾必做）**：
- [ ] 全量越权测试覆盖 `/store/**`、`/api/**` 所有接口
- [ ] 下单接口压测：单节点 500 QPS，P95 < 800ms
- [ ] 一次完整安全 review：支付密钥回显、SQL注入、XSS

> **🚀 M1 出口标准 = 小范围试点交付**：用泛域名 + 自填 AppID 模式，找1~2个真实/模拟商家跑通"开店→上架→下单→收款→发货→售后"的完整商业闭环。

---

## M2 会员与营销（S7~S10，8周）

**目标**：能做活动。每接入一个营销玩法，都要给价格引擎补一批**组合场景回归测试**——这是历史上电商系统最容易因为"改A坏B"翻车的地方。

### Sprint 7（第14~15周）— 会员体系

| 模块 | 任务 |
|---|---|
| BE | `user`/`user_balance_log`/`user_points_log`/`user_grade` 表；会员等级自动升级定时任务；余额充值（`recharge_plan`/`recharge_order`） |
| FE-store | 会员管理列表 + 详情抽屉 | 原型 `store/member-list.html` |
| Client | 我的页资产展示、充值页、会员等级页 | 原型 `h5/my.html` |

### Sprint 8（第16~17周）— 优惠券 + 满减

| 模块 | 任务 |
|---|---|
| BE | `coupon`/`user_coupon` 表 + 领取/核销逻辑 |
| BE | ★价格引擎接入 `CouponHandler`、`FullReduceHandler`（严格遵循责任链顺序：活动价→会员折扣→满减→优惠券→积分→运费） |
| BE | 优惠分摊逆向逻辑联调：退款按分摊比例退，补充组合场景单测 |
| FE-store | 优惠券管理（新建弹窗） | 原型 `store/marketing-coupon.html` |
| Client | 领券中心、确认订单页优惠券选择 | 原型 `h5/coupon-center.html` |

**验收**：多优惠同时生效时，分摊到每个商品行的金额总和=实付金额；退款金额与优惠占比一致，不出现四舍五入误差累积。

### Sprint 9（第18~19周）— 秒杀 + 限时折扣

| 模块 | 任务 |
|---|---|
| BE | `seckill_time`/`seckill_active`/`seckill_goods` 表；Redis Lua脚本预扣库存 + 异步落库对账 |
| BE | `ActivityPriceHandler` 接入价格引擎 |
| FE-store | 秒杀活动配置（场次+商品+限购） | 原型 `store/marketing-seckill.html` |
| Client | 秒杀专场页（倒计时） | 原型 `h5/seckill.html` |

**压测节点**：秒杀下单接口并发压测，验证 Redis 预扣 + 异步落库不超卖。

### Sprint 10（第20~21周）— 拼团 + 砍价 + 签到/积分商城

| 模块 | 任务 |
|---|---|
| BE | `group_active`/`group_record` 拼团状态机（发起/参团/成团/超时退款） |
| BE | `bargain_active`/`bargain_record`；`sign_config` 签到；`points_goods` 积分商城；`goods_comment` 评价 |
| FE-store | 拼团/砍价活动管理 | 原型 `store/marketing-group.html`、`marketing-bargain.html` |
| Client | 拼团详情、砍价页、签到页、积分商城 | 原型 `h5/group-buy.html`、`bargain.html`、`sign-in.html`、`points-mall.html` |

> **M2 出口标准**：优惠券/秒杀/拼团/砍价四大玩法全部跑通，价格引擎责任链完整覆盖文档三 §4 定义的8个 Handler，且有回归测试集固定下来防止后续改动破坏叠加逻辑。

---

## M3 分销与门店（S11~S13，6周）

**目标**：能裂变。佣金结算与退款联动是本阶段复杂度最高的逆向逻辑，安排双人 review。

### Sprint 11（第22~23周）— 分销核心

| 模块 | 任务 |
|---|---|
| BE | `dealer_user`/`dealer_order`/`dealer_setting` 表；下单时预生成待结算佣金记录；推荐关系树绑定 |
| FE-store | 分销中心（分销商列表/分销设置） | 原型 `store/distribution.html` |
| Client | 分销中心（申请/我的团队） | 原型 `h5/distribution-center.html` |

### Sprint 12（第24~25周）— 佣金结算与提现

| 模块 | 任务 |
|---|---|
| BE | 佣金结算定时任务（确认收货 + 售后期满后转可提现） |
| BE | `dealer_withdraw` 提现审核流程 |
| BE | ★退款联动扣回已发放佣金（**双人 code review + 专项测试用例**） |
| FE-store | 分销订单/提现审核 Tab | 原型 `store/distribution.html` |
| Client | 佣金明细、提现申请 |

### Sprint 13（第26~27周）— 门店自提

| 模块 | 任务 |
|---|---|
| BE | `offline_store`/`store_clerk`/`verify_log`；核销码生成与校验（防重复核销）；店员数据权限（仅本店订单） |
| FE-store | 门店管理（门店/店员/核销记录）；员工角色权限中"门店店员"角色配置 | 原型 `store/store-offline.html`、`staff.html` |
| Client | 门店选择页、核销码展示 | 原型 `h5/store-locator.html` |

> **M3 出口标准**：分销全链路（推广→下单→结算→提现）与门店自提（选店→下单→核销）各安排一次完整人工验收，重点验证"退款后佣金正确扣回"不出现资金对不上的情况。

---

## M4 装修与多端（S14~S16，6周）

**目标**：能像自己的店。装修编辑器的组件已经在原型阶段全部定义清楚（含客服悬浮组件、砍价组件等），前端开发直接照原型复刻交互，不需要重新设计。

### Sprint 14（第28~29周）— DIY 引擎后端

| 模块 | 任务 |
|---|---|
| BE | `diy_page`/`diy_template`/`diy_tabbar` 表；页面 JSON Schema 校验；组件白名单按套餐 `menus` 过滤 |
| BE | 草稿/发布双版本、页面复制 |
| FE-store | 装修页面管理列表 | 原型 `store/diy-page-list.html` |

### Sprint 15（第30~31周）— DIY 编辑器

| 模块 | 任务 |
|---|---|
| FE-store（主） | 拖拽编辑器（左组件库/中预览/右属性面板），**含已定义的全部组件：轮播图/导航宫格/商品组/优惠券组/秒杀/拼团/砍价/客服悬浮组件** | 原型 `store/diy-editor.html` |
| FE-store | 底部导航配置器（2~5项，拖拽排序，实时预览） | 原型 `store/diy-tabbar.html` |
| BE | 行业模板库数据、页面渲染 API（端上按组件名实时取数，避免装修数据与商品数据脱节） |

### Sprint 16（第32~33周）— 多端与自定义域名

| 模块 | 任务 |
|---|---|
| BE | 公众号接入（网页授权/模板消息） |
| BE | APP 云打包任务队列（uni-app 云打包对接） |
| BE | 自定义域名审核 + CNAME校验 + Let's Encrypt自动签发 + 网关热加载 |
| FE-admin | 域名管理审核页 | 原型 `admin/domain-list.html` |
| FE-store | 客户端管理（小程序自填/公众号/H5域名/APP打包） | 原型 `store/client.html` |
| Client | 条件编译适配H5/公众号差异（登录方式/支付方式/分享路径） |

> **M4 出口标准**：任选一个试点商家，完整走一遍"套用行业模板 → 自定义装修 → 配置底部导航 → 绑定自己的域名 → 上线"，全程不需要开发介入。

---

## M5 SaaS 运营（S17~S19，6周）

**目标**：能规模化交付。**微信开放平台第三方平台资质申请周期1~2周，需要在Sprint 17开始前就提交**，与开发并行，避免卡在最后阶段。

### Sprint 17（第34~35周）— 微信第三方平台托管

| 模块 | 任务 |
|---|---|
| BE | `component_verify_ticket` 刷新任务（每10分钟） |
| BE | 代码模板注入 `ext.json` 机制（业务代码不允许有租户分支） |
| BE | `mp_authorizer` 授权状态管理 |
| FE-store | 客户端-小程序授权托管模式 UI（已在Sprint16的client.html里预留了这个分支，本sprint补全真实逻辑） | 原型 `store/client.html` |

### Sprint 18（第36~37周）— 批量发布与运维

| 模块 | 任务 |
|---|---|
| BE | `mp_release_task` 任务表+状态机（待上传→已上传→待审→审核中→通过→已发布/驳回） |
| BE | 批量发布队列（限速、可暂停、单个重试） |
| BE | XXL-Job 全量定时任务清单落地（文档三 §6，共18个定时任务） |
| FE-admin | 客户端管理-批量发布任务页、运维中心（定时任务/队列/备份/缓存/用量统计） | 原型 `admin/mp-client.html`、`ops-center.html` |

### Sprint 19（第38~39周）— 在线订购与全链路收尾

| 模块 | 任务 |
|---|---|
| BE | 平台自有商户号收款（订购/续费/升级/增值服务下单） |
| BE | 发票申请与开票记录 |
| BE | 数据备份策略落地 |
| BE | ★**全链路压测**：验证容量目标（1000租户/500万商品/日订单10万）与可用性目标（99.9%） | 文档一 §7 |
| FE-admin | 订购管理（退款审核/发票） | 原型 `admin/order-list.html` |
| FE-store | 我的套餐（在线续费/升级支付） | 原型 `store/my-package.html` |

> **M5 出口标准**：全链路压测达标，具备"从0到批量开店"的规模化能力，正式对外规模化招商。

---

## 质量门禁（贯穿全程，不是可选项）

| 检查点 | 时机 | 一票否决条件 |
|---|---|---|
| 越权测试 | 每个 Sprint 末 | 任一接口跨租户访问未返回403/404 |
| 价格引擎回归测试 | 每次新增/修改 Handler | 组合场景单测未覆盖新增的叠加顺序 |
| 安全 review | M1末、M5末 | 支付密钥可回显明文、SQL注入、XSS |
| 资金类逆向逻辑双人 review | M2末（退款分摊）、M3末（佣金扣回） | 仅单人 review 未走双人复核 |
| 全链路压测 | M1末（试点前）、M5末（规模化前） | 未达到文档一 §7 的性能/容量目标 |

---

## 风险登记表

| 风险 | 影响 | 应对 |
|---|---|---|
| 微信开放平台第三方资质申请慢 | M5 卡进度 | Sprint 17 前提前1~2周提交申请；若逾期，M5 降级为"仅自填 AppID 交付"，托管能力延后 |
| 价格引擎 M1 只搭骨架，M2 密集接入4个新 Handler | 回归测试量陡增 | 每次新增 Handler 强制补充组合场景单测，不允许"先上线后补测试" |
| 分销佣金退款联动逻辑复杂 | 历史高发 bug 区 | S12 强制双人 review + 专项测试用例，上线前人工过一遍典型场景 |
| 3人团队无法并行拆分 BE-A/BE-B 泳道 | 整体周期拉长 | 见下方降级方案 |

---

## 3人团队降级方案

若团队仅3人（2后端 + 1前端全栈/兼client）：

- **无法拆分并行泳道**，所有 Sprint 顺序执行，总周期预估拉长至 **1.4~1.6倍，约13~14个月**
- **优先级重排**：`M0 → M1 → M2 → M4（装修与多端） → M5（运营） → M3（分销与门店）`，先跑通"能卖货 + 能像自己的店 + 能规模化"三件事，分销作为快速跟进的第二阶段单独排期，不阻塞主线招商
- 前端全栈需同时承担 `shop-ui/admin`、`shop-ui/store`、`shop-uniapp` 三端，**前端原型已经全部产出**（见 `docs/prototype/`），可以显著压缩 UI 设计决策的时间消耗

---

## 立即可执行的第一件事

Sprint 0 从今天开始：先把仓库结构和 CI 跑起来，`TenantContext` + 越权测试脚手架是 Sprint 1 唯一不能延期的任务——这是全系统的地基，晚一天补都会在后面滚雪球。
