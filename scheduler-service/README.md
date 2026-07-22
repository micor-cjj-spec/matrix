# scheduler-service

Matrix 定时任务调度中心。Quartz 负责触发，业务事务负责生成执行实例和 Outbox，RabbitMQ 负责向通过 `scheduler-client` 接入的执行器分发。

## 启动前准备

1. 创建 `matrix_scheduler` 数据库。
2. 依次执行：
   - `src/main/resources/db/migration/V1__scheduler_core.sql`
   - `src/main/resources/db/migration/V2__scheduler_executor_reliability.sql`
   - `src/main/resources/db/migration/V3__scheduler_operations_observability.sql`
3. 执行 Quartz 2.x 官方 MySQL InnoDB 建表脚本，保留默认 `QRTZ_` 前缀。
4. `fi-service` 所在业务库执行：
   - `fi-service/src/main/resources/db/migration/V1__scheduler_financial_reports.sql`
5. 在 Nacos 创建 `scheduler-service.yaml`，配置数据源、RabbitMQ 和内部令牌：

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

6. `base-service`、`fi-service` 等执行器服务必须配置相同的 `SCHEDULER_INTERNAL_TOKEN`、`SCHEDULER_BASE_URL` 和 RabbitMQ 连接。
7. 在网关 Nacos 配置中增加路由：

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
        context.reportProgress(50, "CHECKING", "正在检查数据库连接");
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
- 上报 0-100 的执行进度、阶段和说明。

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
- 进度回调只能单调增加，终态实例不再接受进度。

## 人工补偿与告警

运行中心路由：`/scheduler/operations`

支持：

- 立即重试
- 终止重试
- 取消
- 跳过
- 人工标记成功
- 操作审计日志
- 最终失败、超时、DEAD 和执行器离线告警
- 告警确认

当前告警已完成落库与页面处理，尚未接通 IM、邮件或飞书投递。

## 已接入 Handler

### base-service

- `database-health-check`

### fi-service

- `voucher-period-check`
- `financial-report-generate`
- `period-close-precheck`

财务任务通用参数：

```json
{
  "period": "2026-07",
  "bookId": "optional-book-id"
}
```

## 监控

Actuator Prometheus 指标包括：

```text
matrix_scheduler_execution_active
matrix_scheduler_execution_waiting
matrix_scheduler_execution_retry_wait
matrix_scheduler_executor_online
matrix_scheduler_outbox_pending
matrix_scheduler_alert_pending
```

## 验证

```bash
mvn -B -pl scheduler-service,base-service,fi-service -am test
```

前端：

```bash
npm ci
npm run build
```

## 当前边界

- 已实现调度控制面、执行器接入、幂等、心跳、失败重试、超时、SERIAL 排队、执行进度、人工补偿、告警落库和运行看板。
- 已接入基础服务与财务服务真实 Handler。
- 尚未完成生产部署联调、告警渠道投递、协作式取消、任务分片和 DAG。
