# 业务规则

1. RabbitMQ 交付语义为至少一次，消费端必须以 `executionNo` 建数据库唯一键。
2. 自动重试必须创建新 execution，不覆盖原执行记录。
3. `retryCount` 表示初始执行之外允许创建的重试次数。
4. 重试间隔按 `retryIntervalSeconds * 2^(attemptNo-1)` 计算，最大不超过一小时。
5. RUNNING 超过任务 `timeoutSeconds` 进入 TIMEOUT 或 RETRY_WAIT。
6. CREATED/QUEUED 超过调度中心投递超时阈值进入 DISPATCH_TIMEOUT。
7. SERIAL 任务必须通过数据库条件更新竞争唤醒，不能只依赖本地锁。
8. 注册、心跳和回调必须携带 `X-Scheduler-Internal-Token`。
9. 回调的 `X-Executor-Code` 必须与 execution 所属执行器一致。
10. Handler 中涉及订单、凭证、付款等真实业务写操作时，还必须使用业务单号做业务幂等；`executionNo` 只能解决消息重复投递。
