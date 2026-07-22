# 定时任务调度流程

## 创建任务

1. 前台或外部系统提交任务。
2. 校验 Cron、时区、并发策略和 Misfire 策略。
3. 外部请求按 `sourceService + requestId` 幂等查询。
4. 保存 `matrix_scheduler_job`。
5. 注册 Quartz JobDetail 和 CronTrigger。
6. 返回任务与下一次执行时间。

## 到期触发

1. Quartz 集群抢占 Trigger。
2. `MatrixDispatchQuartzJob` 读取 `jobId`。
3. 在同一数据库事务中写入执行实例和 Outbox。
4. OutboxPublisher 扫描待发布事件。
5. 发布到 `matrix.scheduler.execute` TopicExchange。
6. 路由键为 `scheduler.execute.{executorCode}`。
7. 发布成功后执行实例进入 `QUEUED`。
8. 执行器回调 `RUNNING`、`SUCCESS` 或 `FAILED`。

## 失败场景

- RabbitMQ 发布失败：Outbox 指数退避，达到上限后进入 `DEAD`。
- 重复触发：执行实例幂等键和唯一索引阻止重复落库。
- 上次执行未结束：非 `PARALLEL` 策略生成 `SKIPPED` 记录。
- 重复回调：执行实例进入终态后直接返回原结果。
