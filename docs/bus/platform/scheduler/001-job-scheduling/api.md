# 定时任务调度 API

## 管理接口

- `POST /api/scheduler/jobs`：创建任务
- `PUT /api/scheduler/jobs/{jobId}`：修改任务
- `GET /api/scheduler/jobs`：分页查询任务
- `GET /api/scheduler/jobs/{jobId}`：任务详情
- `POST /api/scheduler/jobs/{jobId}/pause`：暂停
- `POST /api/scheduler/jobs/{jobId}/resume`：恢复
- `POST /api/scheduler/jobs/{jobId}/run-now`：立即执行
- `DELETE /api/scheduler/jobs/{jobId}`：软删除
- `GET /api/scheduler/cron/preview`：预览未来执行时间

## 执行实例

- `GET /api/scheduler/executions`：分页查询
- `GET /api/scheduler/executions/{executionNo}`：执行详情
- `POST /api/scheduler/callback/executions/{executionNo}`：执行状态回调

## 外部系统创建

`POST /api/scheduler/open/jobs`

请求头：

- `X-Source-Service`：来源服务编码
- `X-Request-Id`：来源系统唯一请求号

相同来源服务和请求号重复提交时返回首次创建的任务。

## RabbitMQ 协议

- Exchange：`matrix.scheduler.execute`
- Routing Key：`scheduler.execute.{executorCode}`
- Message ID：Outbox 的 `eventId`
- 消费幂等键：`executionNo`

消息包含 `executionNo`、`traceId`、`jobCode`、`tenantId`、`executorCode`、`handlerCode`、`executeType`、`timeoutSeconds` 和 `parameters`。
