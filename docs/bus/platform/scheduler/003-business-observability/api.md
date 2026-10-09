# 接口设计

## 进度回调

`POST /api/scheduler/callback/executions/{executionNo}/progress`

请求头：

- `X-Executor-Code`
- `X-Scheduler-Internal-Token`

请求体：

```json
{
  "executorInstance": "fi-service-host-10003",
  "progress": 60,
  "stage": "AGGREGATING",
  "message": "正在按科目汇总借贷发生额"
}
```

## 人工补偿

- `POST /api/scheduler/executions/{executionNo}/retry-now`
- `POST /api/scheduler/executions/{executionNo}/stop-retry`
- `POST /api/scheduler/executions/{executionNo}/cancel`
- `POST /api/scheduler/executions/{executionNo}/skip`
- `POST /api/scheduler/executions/{executionNo}/mark-success`
- `GET /api/scheduler/executions/{executionNo}/operation-logs`

所有写接口请求体均包含必填 `reason`。

## 运行看板

`GET /api/scheduler/dashboard/summary`

## 告警

- `GET /api/scheduler/alerts`
- `POST /api/scheduler/alerts/{alertId}/ack`

## 财务 Handler

### 凭证期间检查

`fi-service / voucher-period-check`

```json
{
  "period": "2026-07"
}
```

当前复用的凭证协同检查尚未提供账簿维度，因此该任务只接受全账簿范围；传入 `bookId` 会明确拒绝。

### 财务报表生成

`fi-service / financial-report-generate`

```json
{
  "period": "2026-07",
  "bookId": "optional-book-id",
  "reportType": "TRIAL_BALANCE"
}
```

`bookId` 可选；为空时汇总全部账簿。当前 `reportType` 默认 `TRIAL_BALANCE`。

### 期末结账预检查

`fi-service / period-close-precheck`

```json
{
  "period": "2026-07"
}
```

当前凭证与总账对照服务尚未提供账簿维度，因此该任务只接受全账簿范围；传入 `bookId` 会明确拒绝。
