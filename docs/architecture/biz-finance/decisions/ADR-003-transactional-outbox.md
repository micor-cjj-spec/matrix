# ADR-003：采用 Transactional Outbox + Inbox 作为业财事件可靠传输机制

- 状态：Accepted
- 日期：2026-08-26

## Context

若业务单据提交后再同步调用中央 Event Service 或直接发 MQ，会出现数据库提交和消息发送之间的不一致：业务成功但事件丢失，或消息已发但业务事务回滚。

Matrix 当前 Scheduler/OpenAPI 已存在本地 Outbox、重试和幂等消费实践，其中 OpenAPI 已具备条件更新抢占和 RabbitMQ Publisher Confirm。

## Decision

Business Event 采用：

```text
Business Transaction
+ Local Outbox Insert
= Same DB Transaction
```

每个事件生产者维护自己的 Outbox；消费者维护 Inbox。

```text
ERP DB
Business Data + Outbox
        ↓
RabbitMQ Topic Exchange
        ↓
FI Inbox
        ↓
Accounting Handler
```

不建设中央 Event Service。

## Reliability Rules

- Outbox 状态：PENDING → SENDING → SENT，失败 FAILED → DEAD。
- 多实例通过条件 UPDATE claim 发送权。
- 使用 Publisher Confirm。
- MQ 语义按 at-least-once 设计。
- Inbox 使用 `consumerCode + eventId` 唯一键去重。
- MQ Listener 只落 Inbox，不执行长事务财务处理。
- 撤销发布新的反向业务事件，不修改旧 Business Event。

## Consequences

优点：

- 保证业务事实与 Outbox 原子落库。
- RabbitMQ 故障不会阻塞已完成的本地业务事务。
- 消费者可重试、可补偿、可审计。

代价：

- 各业务库均需 Outbox 表。
- 需要清理/归档历史 Outbox/Inbox。
- 消费者必须天然支持重复消息和乱序检查。
