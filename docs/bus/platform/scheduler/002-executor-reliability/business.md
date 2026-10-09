# 调度执行器与可靠执行

## 业务目标

将第一期“任务配置与到点触发”扩展为可接入真实业务服务的执行闭环：业务服务自动注册能力，调度中心按执行器路由消息，消费端幂等执行并回传结果，失败任务自动退避重试，失联任务自动超时收口。

## 本期范围

- `scheduler-client` 自动配置模块。
- `@MatrixJobHandler` Handler 注册机制。
- RabbitMQ 执行消息消费。
- 业务服务本地 `executionNo` 唯一键幂等。
- 执行器、实例、Handler 注册和心跳。
- FAILED/TIMEOUT 自动重试及执行链路。
- QUEUED/RUNNING 超时扫描。
- SERIAL 等待队列。
- 内部注册、心跳和回调共享密钥校验。
- `base-service/database-health-check` 首个真实任务。

## 状态说明

当前状态为“代码已实现，待部署联调”。在 MySQL、Quartz、RabbitMQ、Nacos、网关和两个服务完成部署前，不标记为生产验收完成。
