# 接口说明

## 执行器内部接口

### 注册

`POST /api/scheduler/executors/register`

请求头：`X-Scheduler-Internal-Token`

请求体包含 executorCode、executorName、instanceId、maxConcurrency 和 handlers。

### 心跳

`POST /api/scheduler/executors/heartbeat`

请求头：`X-Scheduler-Internal-Token`

### 执行回调

`POST /api/scheduler/callback/executions/{executionNo}`

请求头：

- `X-Scheduler-Internal-Token`
- `X-Executor-Code`

状态支持 RUNNING、SUCCESS、FAILED、TIMEOUT、CANCELLED。

## 管理接口

- `GET /api/scheduler/executors`
- `GET /api/scheduler/executors/{executorCode}/instances`
- `GET /api/scheduler/executors/{executorCode}/handlers`

管理接口继续走用户 JWT 鉴权。
