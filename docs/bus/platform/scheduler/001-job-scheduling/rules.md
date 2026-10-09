# 定时任务调度规则

1. Quartz 只负责产生执行事件，不直接承载耗时业务。
2. 每次触发必须先生成执行实例，禁止仅记录应用日志。
3. 数据库与 RabbitMQ 之间使用 Outbox 保证最终一致。
4. 投递语义为至少一次，消费端必须以 `executionNo` 做幂等。
5. 前端不得配置任意 Java 类名或方法名，只能使用 `executorCode + handlerCode`。
6. Cron 必须包含时区，修改 Cron 后必须重新注册 Trigger。
7. `SKIP` 和 `SERIAL` 在已有运行中实例时不得产生新的执行消息。
8. 任务暂停后不接受 Cron 触发，但允许保留历史执行记录。
9. OpenAPI 创建必须携带 `X-Source-Service` 和 `X-Request-Id`。
10. 执行终态包括 `SUCCESS`、`FAILED`、`TIMEOUT`、`CANCELLED`、`DEAD`、`SKIPPED`。
11. 请求和响应参数中的密码、令牌、密钥必须在日志和页面中脱敏。
12. 第一阶段不允许小于平台最小间隔的高频 Cron；具体阈值由配置中心控制。
