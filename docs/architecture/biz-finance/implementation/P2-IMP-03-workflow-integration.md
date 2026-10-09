# P2-IMP-03 报价与销售合同 Workflow 对接（开发中）

> 2026-10-09。基于已合并的 #93/#94（Matrix）与 #65/#66（Matrix Web）。
> 实现代码位于新 Draft PR #95；前端为对应 Draft PR #67。所有新开关默认关闭。
> **尚未做真实数据库迁移、Nacos 配置、HTTPS 内部调用或端到端验收，禁止生产启用。**

## 业务时序

```text
用户 JWT --> ERP /sales/{quotes|contracts}/{id}/submit
  -> 锁定业务单据，写入 SUBMITTED + 审计 + WorkflowLink(PENDING)
  -> 本地事务提交
  -> SalesWorkflowStartDispatcher 抢占任务、签名 POST Workflow 内部启动接口
  -> Workflow 按确定性 Idempotency-Key 创建/复用流程实例
  -> ERP 记录 workflow instanceId，状态 ACTIVE
  -> Workflow 任务中心执行审批（校验实际审批人的 JWT、在线会话、角色、租户组织）
  -> Workflow wf_event_outbox 签名回调 ERP /internal/sales/workflow/callback
  -> ERP 验签、校验 300s 时效、eventId、租户/组织/单据/instanceId
  -> 同一 ERP 事务：锁定 link、更新业务状态、追加审计、写 Outbox、标记 link 终态
```

Workflow Outbox 可能先于 ERP 写入 instanceId 发送回调：ERP 返回 409，Workflow Outbox 后续重试；启动请求同一个 Idempotency-Key 可在重试时复用原实例。

## 业务状态

| 业务 | 提交 | Workflow COMPLETED | Workflow REJECTED |
| --- | --- | --- | --- |
| 销售报价 | DRAFT → SUBMITTED | SUBMITTED → APPROVED | SUBMITTED → APPROVAL_REJECTED |
| 销售合同 | DRAFT/DRAFT → DRAFT/SUBMITTED | DRAFT/SUBMITTED → EFFECTIVE/APPROVED | DRAFT/SUBMITTED → DRAFT/REJECTED |

- **前端、销售 REST API 均不得直接 approve**。原本的 `approve` REST 被后端拒绝，即使工作流开关未启用，也不允许重新放行。
- 真实审批人由 Workflow 的 `wf_action_log` 记录；ERP 财务审计的 `foperator_id` 为受控工作流系统操作人，后续可通过 instanceId 汇总完整操作人链路，不要误认系统账号为人工审批人。
- 工作流 `RETURN_TO_INITIATOR`、`resubmit`、`cancel` 对 ERP 销售场景暂时禁用，避免 Workflow / ERP 状态不一致。正式设计需同时增加退回、撤回和再审批实例关联（当前每单唯一流程）。
- 早期遗留的“投标报价”目前仅 `fquote_type=TENDER`，不是完整投标评审系统。

## 安全边界

1. 不信任 URL 角色、`X-User-Id`、`X-User-Roles` 或 `X-Operator-Id`。销售待办按 auth-service 同源签名 JWT、在线 Redis 会话、销售角色和 Redis 授权修订号鉴权；Workflow 任务真正执行人不得等于发起人。
2. Workflow 公开 `/workflow/instances` 不允许创建 `sourceSystem=MATRIX_ERP` 且业务类型为 `SALES_QUOTE/SALES_CONTRACT` 的实例。只有 HMAC 签名的内部端点 `/workflow/internal/sales/instances` 可创建，并强制覆盖回调 URL 为受控配置。
3. 回调使用现有 Workflow HMAC 格式 `HMAC-SHA256(timestamp + "." + rawBody)`；服务端常量时间比较、300 秒时间窗口，并检验数据库流程关联、业务类型、eventId 与事件状态。相同事件重复投递幂等；异类终态事件拒绝并保留人工排查。
4. `MATRIX_SALES_WORKFLOW_INTERNAL_SECRET` 与 `WORKFLOW_CALLBACK_SECRET` 应是**不同的**不少于 32 字节的随机受控密钥。均不能使用默认值或入库到 Git。
5. Workflow 仍有其他通用接口和旧回调消费逻辑；必须通过网络 ACL/API 网关限制对 Workflow 服务、ERP 内部回调的直接访问。仅靠源 IP 或 HTTP Header 无法代替认证。
6. 同一个 Redis（含 database、序列化格式）需可被 auth-service、ERP、Workflow 访问；还应验证角色撤销时三个服务均拒绝旧令牌。
7. 若用户要求恢复工作流，必须先检查两端实例、事件、链接和审计一致性，再做受控重试，不得手工越过签名或授权。

## 部署前配置清单

数据库：

- ERP 数据库依次包含 `012-sales-quotation-contract/schema.sql`、`013-sales-action-audit/schema.sql` 和新的 `014-sales-workflow-link/schema.sql`；
- Workflow 数据库已初始化 `workflow_v1.sql`，并按环境演进部署了后续迁移；
- 在 Workflow 发布两个符合本期审批组织规则的定义，分别供销售报价与合同使用。

密钥、URL 和开关（通过受控 Secret/Nacos 设置，下面仅列变量名）：

| 环境 | 配置 | 要求 |
| --- | --- | --- |
| ERP | `MATRIX_SALES_WORKFLOW_ENABLED` | 默认 false；迁移/联调通过后才能置 true |
| ERP | `MATRIX_SALES_QUOTE_WORKFLOW_KEY`、`MATRIX_SALES_CONTRACT_WORKFLOW_KEY` | 已发布的定义 Key |
| ERP | `MATRIX_SALES_WORKFLOW_START_URL` | 内部 TLS URL，指向 Workflow `/api/workflow/internal/sales/instances` |
| ERP | `MATRIX_SALES_WORKFLOW_INTERNAL_SECRET` | 与 Workflow 内部调用验签 Secret 一致 |
| ERP | `WORKFLOW_CALLBACK_SECRET` | 与 Workflow 出站回调签名 Secret 一致 |
| ERP | `MATRIX_SALES_WORKFLOW_SYSTEM_ACTOR_ID` | 正整数、专门的系统操作人，不可等于普通制单人 |
| Workflow | `MATRIX_SALES_WORKFLOW_INTERNAL_ENABLED` | 默认 false |
| Workflow | `MATRIX_SALES_WORKFLOW_CALLBACK_URL` | 内部 TLS URL，指向 ERP `/api/internal/sales/workflow/callback` |
| Auth/ERP/Workflow | `security.jwt.secret`、Redis 连接信息 | 同一签名密钥和同一个受控会话/授权版本 Redis |
| 现有销售 | `matrix.sales.commercial-enabled`、`matrix.sales-auth.issuer-enabled` | 原开关仍独立控制，不因本期代码自动打开 |

## 验收与已知限制

已写单元回归覆盖签名校验、过期签名、跨租户/组织与单据不匹配、重复事件、错误终态、无绑定实例、同人审批、权限撤销、幂等链接，以及关闭直接审批路径。CI 的 `Sales Workflow Integration CI` 将 ERP 与 Workflow 模块统一编译测试。

**尚需在专门的 MySQL/Redis/HTTPS 测试环境跑完整 E2E：** 提交、异步启动、同幂等键重试、回调早于实例绑定、审批通过/拒绝、DB 回滚、真实待办鉴权、角色撤销、异常恢复与审计。尚未做完前此 PR 保持 Draft。

尤其注意：`wf_event_outbox` 原有的 SENDING 卡住恢复策略需要在部署检查中核对；不能把模拟的 Mockito 测试当成跨服务投递保证。
