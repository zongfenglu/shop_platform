# 贡献指南

## 分支策略

- `main`：生产可发布状态，只接受 `develop` 合并
- `develop`：集成分支，日常开发的合并目标
- `feature/xxx`：功能分支，从 `develop` 切出，完成后 PR 回 `develop`
- `hotfix/xxx`：生产紧急修复，从 `main` 切出，修完后同时合回 `main` 和 `develop`

## 提交前自检

```bash
./mvnw compile   # 先确认能编译
./mvnw verify    # 跑单测 + ArchUnit 静态检查 + 跨租户越权集成测试（*IT 结尾，需要本机 Docker 可用）
```

CI 会重复以上两步，本地先过一遍能避免来回等 CI。

> Windows + Docker Desktop 用户注意：`*IT` 集成测试依赖 Testcontainers 拉起真实 MySQL，
> 部分 npipe 配置下会报 "Could not find a valid Docker environment"（已知的 Windows 兼容性问题，
> 非测试逻辑缺陷）。本地受阻时可先跑 `./mvnw test`（仅单测+ArchUnit），`*IT` 交给 CI（Linux runner）验证；
> 但不要因为本地跑不过就删掉或跳过这条测试。

## PR Checklist

提交 PR 前请确认：

- [ ] 涉及租户数据访问的接口，是否走了 `TenantSafeService#getByIdWithTenant`（而不是裸用 `getById`）？
- [ ] 新增的写操作（update/delete）是否带了明确的 shop_id 过滤条件（`BlockAttackInnerInterceptor` 会拦截无 where 条件的全表操作，但业务上仍需确认过滤的是"当前租户"而非误用其他条件）？
- [ ] 是否有异步任务 / `@Async` / MQ 消费 / 线程池场景？如有，是否正确设置并在 `finally` 中清理了 `TenantContext`？
- [ ] 新增的营销/价格相关逻辑，是否接入了价格计算引擎的责任链（而不是另起一套算价逻辑）？见文档三 §4。
- [ ] 支付密钥、AK/SK 等敏感配置是否走了加密存储，接口是否只返回掩码？
- [ ] 是否补充了对应的单元测试？跨租户访问场景是否补充了越权测试（命名以 `IT` 结尾，走 failsafe；可参考继承/复用 `AbstractTenantAccessTest` 的断言口径）？

## Code Review 关注点

- **资金类逆向逻辑**（退款分摊、佣金扣回）按团队约定需双人 review，见 `docs/04-开发计划-Sprint排期.md` 质量门禁章节。
- 领域包（`shop-domain` 下各业务包）之间不允许反向依赖，发现耦合应先讨论边界而不是加依赖绕过。
- 新增定时任务需确认：全租户循环类任务是否做了按 shop_id 分片，单租户异常是否会中断整批处理。
- **前端拿到的所有 id 字段都是字符串，不是数字**——`shop-framework` 的 `JacksonConfig` 把全平台 `Long`
  统一序列化成 JSON 字符串（见该类注释）：雪花 ID 是 19 位数字，超过 JS `Number.MAX_SAFE_INTEGER`（约16位），
  按数字传给前端会被 `JSON.parse` 静默舍入精度，前端拿着被舍入过的 id 请求详情/更新接口，后端查不到，
  报的是"无权限访问"或"资源不存在"，现象和真越权一模一样，很容易被带偏排查方向。前端不需要因此做任何
  特殊处理——id 本来就只用于展示和原样回传，从不参与数值运算；但新写代码时如果对 id 做了
  `Number(id)`、`id + 1`、`id > xxx` 这类操作，就是在重新引入这个问题。

## 提交信息约定

采用 `<type>: <description>` 格式，type 取值：`feat`/`fix`/`refactor`/`test`/`docs`/`chore`。
