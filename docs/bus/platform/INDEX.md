# 平台能力业务文档索引

## 已建立模块

- `scheduler/001-job-scheduling`：定时任务调度平台

## 当前状态

定时任务调度已完成第一阶段前后端代码：

- 后端 `scheduler-service`
- 前端 `/scheduler/jobs`
- Quartz JDBC 集群配置
- 执行实例和 Outbox
- RabbitMQ 调度消息
- 外部系统幂等创建接口
- 执行状态回调

部署前仍需在目标环境配置 Nacos 数据源、网关路由、Quartz 官方表和 RabbitMQ。
