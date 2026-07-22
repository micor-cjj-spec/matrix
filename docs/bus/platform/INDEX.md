# 平台能力业务文档索引

## 已建立模块

- `scheduler/001-job-scheduling`：定时任务配置、Quartz 触发、执行实例与 Outbox。
- `scheduler/002-executor-reliability`：执行器接入、幂等消费、心跳、重试、超时与 SERIAL 队列。

## 当前状态

调度平台已经完成两阶段代码：

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

当前状态为代码已实现、待部署联调。目标环境仍需执行 V1/V2 SQL、Quartz 官方表，并配置 Nacos、网关、RabbitMQ 和统一 `SCHEDULER_INTERNAL_TOKEN`。
