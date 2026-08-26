# P0-03 Business Event 业务事件中心设计 v1

> 状态：Draft v1  
> 归档日期：2026-08-26

## 1. 定位

Business Event 表达一个已经发生、对其他业务域有意义、不可否认的业务事实。

推荐：

```text
PURCHASE_RECEIPT_CONFIRMED
SUPPLIER_INVOICE_CONFIRMED
PAYMENT_COMPLETED
SALES_DELIVERY_CONFIRMED
CUSTOMER_RECEIPT_CONFIRMED
```

不推荐：

```text
PROCESS_RECEIPT
CREATE_AP
DO_PAYMENT
```

事件命名使用过去式业务事实，而不是命令。

## 2. 与 Accounting Event 的边界

```text
Business Event
= 发生了什么

Accounting Event
= 这件事在财务上意味着什么

Accounting Rule
= 应该怎么记账
```

例如：

```text
PURCHASE_RECEIPT_CONFIRMED
        ↓
PURCHASE_RECEIPT_ESTIMATE_RECOGNITION
        ↓
Accounting Rule
        ↓
Voucher
```

Business Event 不应直接命名为 `CREATE_ESTIMATED_AP_VOUCHER`。

## 3. 不建设中央 Event Service

不能使用：

```text
erp-service
→ HTTP event-service
→ event DB
→ MQ
```

否则业务数据库提交成功而事件服务调用失败时会丢事件。

采用 Transactional Outbox：

```text
业务单据更新
+
Business Event Outbox insert
=
同一本地数据库事务
```

因此每个生产者在自己的数据库保存 Outbox：

```text
matrix_erp_business_event_outbox
matrix_base_business_event_outbox
matrix_fi_business_event_outbox
```

## 4. Common 只放契约

建议：

```text
common/single/cjj/bizfi/event
├─ BusinessEventEnvelope
├─ AggregateRef
├─ EventDocumentRef
└─ BusinessEventMetadata
```

不要把 RabbitTemplate、Mapper、Outbox Entity 放入 common。

## 5. Event Envelope

```java
public record BusinessEventEnvelope<T>(
    String eventId,
    String eventType,
    Integer eventVersion,
    String producerService,
    String domainCode,
    String tenantId,
    Long orgId,
    Long accountingOrgId,
    AggregateRef aggregate,
    EventDocumentRef sourceDocument,
    LocalDate businessDate,
    String correlationId,
    String causationId,
    String traceId,
    Long operatorId,
    LocalDateTime occurredAt,
    T payload
) {}
```

Aggregate：

```java
public record AggregateRef(
    String aggregateType,
    String aggregateId,
    Long aggregateVersion
) {}
```

Document：

```java
public record EventDocumentRef(
    String systemCode,
    String documentType,
    String documentId,
    String documentNo
) {}
```

## 6. 两种 Version

```text
aggregateVersion
= 业务对象版本

eventVersion
= 事件契约版本
```

两者不能混用。

## 7. Event Type 不做中央大 Enum

每个领域自己维护事件类型常量，例如：

```text
ProcurementEventTypes
SalesEventTypes
FinanceEventTypes
```

Common 只约束命名、Envelope 和版本规则。

## 8. Payload 保存业务事实快照

事件不能只发一个 `receiptId` 再让财务回查 ERP，否则消息延迟期间业务数据变更会导致财务处理的不是事件发生时的事实。

Payload 应保存足以核算的快照，但不能直接序列化 Persistence Entity。

示例：

```text
receiptId / receiptNo
businessPartnerId / code / name
currency
amount / tax
lines
projectId
costCenterId
```

Integration Contract 与 Persistence Entity 必须分离。

## 9. Outbox 状态

```text
PENDING
→ SENDING
→ SENT
```

失败：

```text
SENDING
→ FAILED
→ retry
→ DEAD
```

服务宕机卡在 SENDING 时，超时恢复为 FAILED。

多实例通过条件 UPDATE 抢占发送权，不能 SELECT 后直接发送。

## 10. RabbitMQ 拓扑

建议 Topic Exchange：

```text
matrix.business.event.exchange
```

Routing Key：

```text
biz.{domain}.{object}.{fact}
```

例如：

```text
biz.procurement.purchase_receipt.confirmed
biz.procurement.purchase_receipt.reversed
biz.procurement.supplier_invoice.confirmed
biz.finance.payment.completed
```

每个消费者独立 Queue，不共享同一 Queue 竞争消费。

## 11. FI Inbox

消费侧引入：

```text
matrix_fi_business_event_inbox
```

MQ Listener 只做：

```text
校验 Envelope
→ INSERT Inbox（幂等）
→ ACK MQ
```

不要在 RabbitListener 里同步完成规则匹配、凭证生成、过账。

Inbox 唯一键：

```text
consumerCode + eventId
```

状态：

```text
PENDING
→ PROCESSING
→ SUCCEEDED
```

异常：

```text
FAILED
DEAD
IGNORED
```

## 12. Handler Registry

```java
interface BusinessEventHandler<T> {
    String eventType();
    int supportedVersion();
    void handle(BusinessEventEnvelope<T> event);
}
```

按 `eventType + eventVersion` 唯一选择 Handler。

## 13. 撤销事件

不能修改旧 Event 状态来表示撤销。

正确：

```text
EVT001 PURCHASE_RECEIPT_CONFIRMED

EVT009 PURCHASE_RECEIPT_REVERSED
       originalEventId = EVT001
```

Business Event 是历史事实，撤销也是一个新事实。

## 14. Workflow / BOTP 边界

Workflow 只表达审批动作，真正的 Business Event 必须由业务聚合所属服务发布。

BOTP 创建 `PurchaseReceipt(DRAFT)` 时通常不产生财务 Business Event；当 Receipt 达到业务确认状态后才发布 `PURCHASE_RECEIPT_CONFIRMED`。

## 15. P0 首批事件

```text
PURCHASE_RECEIPT_CONFIRMED
PURCHASE_RECEIPT_REVERSED
SUPPLIER_INVOICE_CONFIRMED
SUPPLIER_INVOICE_REVERSED
PAYMENT_COMPLETED
PAYMENT_REVERSED
```

## 16. 验收

- 业务确认与 Outbox 同事务。
- Outbox 插入失败则业务一起回滚。
- RabbitMQ 停机时业务可提交，Outbox 保留并重试。
- 多实例只允许一个 Publisher claim。
- Broker ACK 超时进入 FAILED。
- 同一 MQ 消息重复发送，Inbox 只保留一条。
- FI 故障后 Inbox 可重试。
- 撤销必须发布新 Event，不修改旧 Event。
