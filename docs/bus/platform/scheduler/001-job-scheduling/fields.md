# 定时任务调度字段说明

## 任务定义

| 字段 | 说明 |
|---|---|
| `fjob_code` | 平台内唯一任务编码 |
| `fsource_type` | `PLATFORM`、`OPEN_API` 或 `SYSTEM` |
| `fsource_service` | 创建任务的来源服务 |
| `fidempotency_key` | 外部请求幂等键 |
| `fcron_expression` | Quartz Cron 表达式 |
| `ftimezone` | IANA 时区，默认 `Asia/Shanghai` |
| `fexecute_type` | 当前主要使用 `MQ`，预留 `INTERNAL_HANDLER`、`HTTP` |
| `fexecutor_code` | 执行器或目标服务编码 |
| `fhandler_code` | 业务处理器编码 |
| `fexecute_parameters` | JSON 执行参数 |
| `fconcurrency_policy` | `SKIP`、`SERIAL`、`PARALLEL` |
| `fmisfire_policy` | `DO_NOTHING`、`FIRE_ONCE_NOW`、`FIRE_ALL` |
| `fstatus` | `ENABLED`、`PAUSED`、`DELETED` |

## 执行实例

| 字段 | 说明 |
|---|---|
| `fexecution_no` | 对外执行编号 |
| `fscheduled_time` | 原计划触发时间 |
| `ftrigger_type` | `CRON` 或 `MANUAL` |
| `fstatus` | `CREATED`、`QUEUED`、`RUNNING`、终态或 `SKIPPED` |
| `ftrace_id` | 跨服务链路标识 |
| `fidempotency_key` | 单次触发去重键 |

## Outbox

| 字段 | 说明 |
|---|---|
| `fevent_id` | RabbitMQ Message ID |
| `frouting_key` | `scheduler.execute.{executorCode}` |
| `fstatus` | `PENDING`、`SENT`、`FAILED`、`DEAD` |
| `fretry_count` | 已尝试发布次数 |
| `fnext_retry_time` | 下一次指数退避时间 |
