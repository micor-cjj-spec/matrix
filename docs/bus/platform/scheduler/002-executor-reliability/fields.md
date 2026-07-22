# 字段说明

## matrix_scheduler_execution 新增字段

| 字段 | 说明 |
|---|---|
| `froot_execution_id` | 一次原始执行及其所有重试的根执行 ID |
| `fparent_execution_id` | 当前重试实例的直接父执行 ID |
| `fnext_retry_time` | RETRY_WAIT 状态的下次重试时间 |
| `fdeadline_time` | RUNNING 状态的执行超时时间 |
| `fattempt_no` | 当前为第几次尝试，初始执行为 1 |

## matrix_scheduler_executor_instance

记录执行器的具体服务实例：执行器编码、实例 ID、在线状态、最大并发数、运行数和最后心跳时间。

## matrix_scheduler_handler

记录执行器声明的任务处理能力。任务保存时必须选择状态为 ENABLED 的 Handler。

## matrix_scheduler_execution_record

由 `scheduler-client` 在业务服务数据库中创建，`fexecution_no` 唯一，用于阻止 RabbitMQ 重复投递造成重复执行。
