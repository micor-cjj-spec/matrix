# scheduler-service

Matrix 定时任务调度中心。Quartz 负责触发，业务事务负责生成执行实例和 Outbox，RabbitMQ 负责向通过 `scheduler-client` 接入的执行器分发。

## 启动前准备

1. 创建 `matrix_scheduler` 数据库。
2. 依次执行：
   - `src/main/resources/db/migration/V1__scheduler_core.sql`
   - `src/main/resources/db/migration/V2__scheduler_executor_reliability.sql`
3. 执行 Quartz 2.x 官方 MySQL InnoDB 建表脚本，保留默认 `QRTZ_` 前缀。
4. 在 Nacos 创建 `scheduler-service.yaml`，配置数据源、RabbitMQ 和内部令牌：

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

matrix:
  scheduler:
    internal-token: ${SCHEDULER_INTERNAL_TOKEN}
```

5. `base-service` 等执行器服务必须配置相同的 `SCHEDULER_INTERNAL_TOKEN` 和 RabbitMQ 连接。
6. 在网关 Nacos 配置中增加路由：

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

## scheduler-client 接入

业务模块增加依赖：

```xml
<dependency>
  <groupId>single.cjj</groupId>
  <artifactId>scheduler-client</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

实现 Handler：

```java
@Component
@MatrixJobHandler(value = "database-health-check", name = "数据库健康检查")
public class DatabaseHealthCheckJob implements MatrixJob {
    @Override
    public JobResult execute(JobContext context) {
        return JobResult.success();
    }
}
```

客户端自动完成：

- 声明并绑定 `matrix.scheduler.executor.{executorCode}` 队列。
- 注册执行器、实例和 Handler。
- 定时心跳。
- 创建本地 `matrix_scheduler_execution_record` 幂等表。
- 消费消息并回调 RUNNING/SUCCESS/FAILED。

## RabbitMQ 路由

TopicExchange：`matrix.scheduler.execute`

路由键：

```text
scheduler.execute.{executorCode}
```

消费端以 `executionNo` 防止消息重复投递。真实业务写操作还必须使用订单号、付款单号等业务键保证跨重试幂等。

## 可靠性策略

- FAILED/TIMEOUT 自动进入 RETRY_WAIT。
- 每次重试创建新的 execution，保留根执行和父执行关系。
- 重试采用指数退避，最大间隔一小时。
- CREATED/QUEUED 超过投递阈值自动超时。
- RUNNING 超过任务 timeoutSeconds 自动超时。
- SERIAL 任务进入 WAITING，通过数据库条件更新竞争唤醒。
- 执行器超过 90 秒未心跳标记 OFFLINE。

## 验证

```bash
mvn -B -pl scheduler-service,base-service -am test
```

前端：

```bash
npm ci
npm run build
```

## 当前边界

- 已实现调度控制面、执行器接入、幂等、心跳、失败重试、超时和 SERIAL 排队。
- 已接入 `base-service/database-health-check` 作为首个真实 Handler。
- 尚未完成生产部署联调、告警看板、任务分片和 DAG。
