# 平台能力业务文档索引

## 已建立模块

- `scheduler/001-job-scheduling`：定时任务配置、Quartz 触发、执行实例与 Outbox。
- `scheduler/002-executor-reliability`：执行器接入、幂等消费、心跳、重试、超时与 SERIAL 队列。
- `scheduler/003-business-observability`：真实财务任务、执行进度、人工补偿、告警和运行看板。

## 当前状态

调度平台已经完成三阶段代码：

### 第一阶段：调度控制面

- 后端 `scheduler-service`
- 前端 `/scheduler/jobs`
- Quartz JDBC 集群配置
- 执行实例和 Outbox
- RabbitMQ 调度消息
- 外部系统幂等创建接口

### 第二阶段：可靠执行

- 公共模块 `scheduler-client`
- `@MatrixJobHandler` 自动发现
- 业务服务数据库唯一键幂等
- 执行器、实例和 Handler 注册
- 心跳和离线判断
- FAILED/TIMEOUT 指数退避重试
- QUEUED/RUNNING 超时扫描
- SERIAL WAITING 队列
- 内部回调共享密钥认证
- `base-service/database-health-check` 示例任务

### 第三阶段：真实业务与可观测性

- `fi-service/voucher-period-check`
- `fi-service/financial-report-generate`
- `fi-service/period-close-precheck`
- Handler 执行进度与阶段上报
- `/scheduler/operations` 运行中心
- 人工重试、终止重试、取消、跳过和标记成功
- 人工操作审计日志
- 最终失败、超时、DEAD 和执行器离线告警
- 调度运行汇总接口与 Prometheus Gauge

当前状态为代码已实现、待部署联调。目标环境需依次执行调度 V1/V2/V3 SQL、财务报表快照 SQL、Quartz 官方表，并配置 Nacos、网关、RabbitMQ 和统一 `SCHEDULER_INTERNAL_TOKEN`。
