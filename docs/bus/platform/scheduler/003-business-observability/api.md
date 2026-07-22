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

- `fi-service / voucher-period-check`
- `fi-service / financial-report-generate`
- `fi-service / period-close-precheck`

通用参数：

```json
{
  "period": "2026-07",
  "bookId": "optional-book-id"
}
```

报表任务可增加 `reportType`，当前默认 `TRIAL_BALANCE`。
