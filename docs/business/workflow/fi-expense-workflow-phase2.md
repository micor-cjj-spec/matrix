# FI 报销工作流业务闭环（二期）

本期在报销工作流接入一期基础上，补齐退回编辑、提交前影像校验、业务撤销、审批详情和状态对账。

## 业务接口

- `PUT /api/fi/expense-reimbursements/{expenseId}`：仅申请人可修改 `DRAFT` 或 `RETURNED` 单据，使用 `version` 乐观锁。
- `POST /api/fi/expense-reimbursements/{expenseId}/submit`：提交前要求至少一份已确认且扫描状态为 `CLEAN` 的 `INVOICE` 影像。
- `POST /api/fi/expense-reimbursements/{expenseId}/cancel`：写入 `WORKFLOW_CANCEL_REQUESTED` 业务 Outbox，异步调用工作流撤销接口。
- `GET /api/fi/expense-reimbursements/{expenseId}/approval-detail`：聚合单据、绑定、流程实例、影像、任务和时间线。
- `POST /api/fi/expense-reimbursements/admin/reconcile?limit=100`：手动触发状态对账。

## 工作流查询接口

- `GET /api/workflow/instances/{instanceId}/tasks`
- `GET /api/workflow/instances/{instanceId}/timeline`

时间线来自 `wf_action_log`，并关联任务节点名称。

## 自动对账

`ExpenseWorkflowReconciliationService` 默认每十分钟扫描非终态绑定，通过业务键查询工作流事实状态：

- `RUNNING -> APPROVING`
- `WAITING_RESUBMIT -> RETURNED`
- `COMPLETED -> APPROVED`
- `REJECTED -> REJECTED`
- `CANCELLED -> CANCELLED`

状态不一致时自动修复，并写入 `fi_workflow_reconciliation_issue`。查询失败记录为 `OPEN`，自动修复记录为 `AUTO_RESOLVED`。

## 数据库

部署前依次执行：

1. `fi_workflow_expense_v1.sql`
2. `fi_workflow_expense_v2.sql`

## 配置

- `FI_WORKFLOW_RECONCILIATION_DELAY_MS`：自动对账间隔，默认 600000 毫秒。
- `FI_WORKFLOW_BASE_URL`：工作流服务基础地址。
- `WORKFLOW_CALLBACK_SECRET`：工作流与财务服务共享回调签名密钥。

## 一致性边界

编辑和提交使用单据 `version` 乐观锁。启动、重提和撤销均通过业务 Outbox 发送；工作流回调使用事件 ID 幂等；定时对账负责修复回调丢失或短暂失败造成的状态差异。
