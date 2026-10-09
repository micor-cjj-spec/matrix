# P2-IMP-03 第二期：操作审计与审批接入边界

> 2026-10-09，开发分支 `agent/p2-imp-03-approval-audit-20261009`
> 前置：PR #93 已合并 `dev`。销售功能开关仍默认关闭；不表示生产可用。

## 已实现：不可变业务操作审计

新增迁移：`deliverables/erp/013-sales-action-audit/schema.sql`，只在 `matrix_erp` 执行。

- 对报价 CREATE、UPDATE、SUBMIT、APPROVE、SEND、ACCEPT、REJECT、WITHDRAW、CANCEL、EXPIRE 写审计。
- 对合同 CREATE、SUBMIT、APPROVE 写审计。
- 审计记录包含租户、组织、单据类型/ID、动作、前后状态、服务端已认证操作人、服务端时间戳。
- 审计与单据更新、Outbox 在同一数据库事务中；审计插入失败必须中止并回滚业务操作，不允许产生无审计的审批。
- 不提供更新/删除审计接口。建议生产数据库使用仅 INSERT/SELECT 的最小授权与只读归档。
- 审计查询要求先验证 JWT、租户/组织访问权限，再按租户、组织、单据过滤，单次返回最近 200 条。
- 新增审批人与制单人分离校验；**销售角色本身不等于完成 Workflow 任务审批**。

## API

```http
GET /sales/quotes/{id}/audit?tenantId=...
GET /sales/contracts/{id}/audit?tenantId=...
```

## Workflow 正式接入待完成

Workflow 服务已有 `POST /workflow/instances`（带 Idempotency-Key）、`wf_instance`、`wf_task`、`wf_action_log` 和事件 Outbox；但**本批没有启用跨服务审批回调**，不应将目前销售 `approve` 视为已走流程审批。

正式接入需满足：
1. 提交单据时创建幂等工作流实例，`sourceSystem=MATRIX_ERP`、`businessType=SALES_QUOTE/SALES_CONTRACT`，保存流程 instanceId/definitionVersion 及其本地关联，处理 DB / MQ 最终一致性。
2. 仅 Workflow 服务调用的受保护回调可将审批结果写入 ERP；须强制验签 HMAC、校验时效、请求原文、租户/组织/单据类型、instanceId、taskId 和幂等回放。
3. 不能由普通 `SALES_APPROVER` 请求直接模拟 Workflow APPROVE。完成迁移前，正式环境必须保持 `matrix.sales.commercial-enabled=false`。
4. 拒绝/退回/撤回须有相应状态迁移并同步 Workflow 实例，错误或延迟重试不得重复审批及 Outbox 事件。
5. Workflow 回调签名配置当前存在非生产默认值；部署前必须在受控 Secret 中设强随机密钥，并限定回调网络入口。
6. 历史审计向工作流流程时间线关联时应展示两类事件来源，不覆盖、不改写已有审计。

## 必要验收

- Maven: `mvn -q -pl auth-service,erp-service -am test`
- 前端 `npm ci && npm run build`
- 多租户/跨组织读审计拒绝、审批人与制单人相同拒绝、审计写失败事务回滚（集成测试尚待 DB 容器环境验证）。
- 配置隔离：先执行 `012`、`013` 迁移，再在受控测试环境显式启用功能开关。

