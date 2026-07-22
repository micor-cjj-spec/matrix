# 流程说明

## 服务启动

1. 业务服务加载 `scheduler-client`。
2. 扫描实现 `MatrixJob` 且标注 `@MatrixJobHandler` 的 Bean。
3. 创建执行器专属 RabbitMQ 队列并绑定路由键。
4. 初始化本地执行幂等表。
5. 向调度中心注册执行器、实例和 Handler。
6. 每 30 秒发送心跳；超过 90 秒未心跳标记 OFFLINE。

## 正常执行

1. Quartz 触发任务并生成 execution 与 Outbox。
2. Outbox 发布到 `scheduler.execute.{executorCode}`。
3. 业务服务以 `executionNo` 尝试插入本地执行记录。
4. 唯一键冲突时直接 ACK，不重复执行。
5. 回调 RUNNING，调用对应 Handler。
6. 回调 SUCCESS 或 FAILED，并更新本地记录。

## 失败重试

1. FAILED/TIMEOUT 且未超过重试次数时进入 RETRY_WAIT。
2. 根据基础间隔进行指数退避。
3. 扫描器到期后生成新的 execution 和 Outbox。
4. 新实例记录 `rootExecutionId`、`parentExecutionId` 和递增的 `attemptNo`。
5. 达到最大次数后保留最终 FAILED/TIMEOUT，不再创建新实例。

## SERIAL

发生并发时新实例进入 WAITING。扫描器确认同一任务不存在 CREATED、QUEUED、RUNNING、RETRY_WAIT 后，通过 `WHERE status = WAITING` 条件更新竞争唤醒，影响行数为 1 的实例才允许创建 Outbox。
