# scheduler-service

Matrix 定时任务调度中心。Quartz 负责触发，业务事务负责生成执行实例和 Outbox，RabbitMQ 负责向执行器分发。

## 启动前准备

1. 创建 `matrix_scheduler` 数据库并执行 `src/main/resources/db/migration/V1__scheduler_core.sql`。
2. 执行 Quartz 2.x 官方 MySQL InnoDB 建表脚本，保留默认 `QRTZ_` 前缀。
3. 在 Nacos 创建 `scheduler-service.yaml`，至少配置数据源和 RabbitMQ：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/matrix_scheduler?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${SCHEDULER_DB_PASSWORD}
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: guest
    password: ${RABBITMQ_PASSWORD}
```

4. 在网关 Nacos 配置中增加路由：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: scheduler-service
          uri: lb://scheduler-service
          predicates:
            - Path=/scheduler/**
```

网关本身已有 `/api` context-path，前端统一请求 `/api/scheduler/**`。

## RabbitMQ 消费约定

执行器绑定 TopicExchange `matrix.scheduler.execute`：

```text
scheduler.execute.{executorCode}
```

消费端必须以 `executionNo` 做幂等。执行开始和结束时调用：

```text
POST /api/scheduler/callback/executions/{executionNo}
```

## 本地验证

```bash
mvn -pl scheduler-service -am test
mvn -pl scheduler-service -am package
```

## 当前边界

- 已实现动态 Cron、Quartz 集群配置、执行实例、Outbox、MQ 投递和结果回调。
- OpenAPI 当前只实现请求号幂等，HMAC 签名过滤器在下一迭代补齐。
- 自动业务重试、执行器心跳、告警和 DAG 不在本次范围。
