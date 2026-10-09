# P0-02 BOTP 单据关系中心增强设计 v1

> 状态：Draft v1  
> 归档日期：2026-08-26

## 1. 定位

BOTP 负责：

- 单据类型注册
- 转换规则与版本
- 单头/分录字段映射
- 目标单创建幂等
- 上下游关系
- 分录关系
- 反写与关系失效

BOTP 不负责：

- 业务规则本身
- 三单匹配判定
- 会计处理

## 2. 完整 DocumentKey

跨域单据引用禁止只按 `documentId` 查询。

统一：

```java
public record DocumentKey(
    String tenantId,
    String systemCode,
    String documentType,
    String documentId
) {}
```

所有查询、失效和穿透都必须使用完整键。

## 3. DocumentRef

目标结构：

```java
public record DocumentRef(
    String systemCode,
    String documentType,
    String documentId,
    String documentNo,
    List<String> entryIds
) {}
```

`documentNo` 作为历史/展示快照，避免每次穿透都跨服务查询单号。

## 4. RelationType

`matrix_botp_document_relation` 增加：

```text
frelation_type
```

首批：

```text
GENERATES
FULFILLS
SETTLES
REVERSES
ADJUSTS
REFERENCES
```

示例：

```text
PR → PO       GENERATES
PO → Receipt  FULFILLS
AP → Payment  SETTLES
Delivery → Return REVERSES
```

RelationType 属于规则定义，并在关系落库时保存规则版本快照。

## 5. Header Relation 增强

建议新增：

```text
fsource_document_no
frelation_type
fsource_org_id
ftarget_org_id
```

并增加数据库唯一约束，避免并发重复关系：

```text
tenant
+ execution
+ source system/type/id
+ target system/type/id
+ deleteFlag
```

应用层幂等和数据库唯一索引必须同时存在。

## 6. 正式启用 Relation Entry

现有数据库已预留：

```text
matrix_botp_document_relation_entry
```

P0-02 正式接入 Java：

```text
BotpDocumentRelationEntryEntity
BotpDocumentRelationEntryMapper
BotpRelationEntryRepository
```

领域模型应支持：

```text
sourceEntryId
targetEntryId
quantity
baseQuantity
amount
baseAmount
relationStatus
```

## 7. 部分履约

例如：

```text
PO Line1 = 100
```

第一次：

```text
Receipt1 = 60
```

RelationEntry：

```text
PO.Line1 → GR1.Line1 quantity=60
```

第二次：

```text
Receipt2 = 40
```

结果：

```text
received = 100
remaining = 0
status = COMPLETE
```

作废 GR1 后，仅统计 ACTIVE RelationEntry：

```text
received = 40
remaining = 60
status = PARTIAL
```

## 8. 多对一/一对多

复杂关系由 RelationEntry 表达：

```text
PO1.Line1 ─30─┐
              ├→ GR1.Line1
PO2.Line1 ─70─┘
```

不以单一 `purchaseOrderId` 外键强行表达所有履约关系。

业务表可以保留常用 `primary_source_id`，复杂关系以 BOTP 为权威。

## 9. Allocation

不要只支持 amount。

统一：

```java
public record Allocation(
    BigDecimal quantity,
    BigDecimal targetQuantity,
    BigDecimal baseQuantity,
    BigDecimal amount,
    BigDecimal baseAmount
) {}
```

采购/销售主要用数量，应付/付款主要用金额。

## 10. Reservation

并发下推必须防止超额履约。

Adapter 增加概念能力：

```text
reserve
commitReservation
releaseReservation
```

例如剩余 40，两人同时收货 30，只允许第一个预占成功。

BOTP 只记录 allocation 事实；是否允许超收、超收比例、单据是否可继续履约属于业务领域规则。

## 11. TargetResult 与分录映射

目标单创建结果应返回分录 correlationKey：

```java
TargetEntryResult(
    correlationKey,
    targetEntryId
)
```

避免假设“源第 N 行一定对应目标第 N 行”，支持合并、拆分、过滤和重排。

## 12. 标准执行时序

```text
requestId 幂等
→ Load Source
→ Domain Validate
→ Reservation
→ Rule Mapping
→ Create Target
→ Save Header Relation
→ Save Entry Relation
→ Commit Reservation
→ Writeback Source
→ Outbox
→ SUCCEEDED
```

目标创建后关系保存失败时，不重新盲目创建目标单，而利用目标幂等键恢复执行。

## 13. 状态失效与反写

目标单作废/取消/删除/驳回时：

```text
ACTIVE
→ INVALID
→ REVERSING
→ REVERSED
```

失效后重新汇总有效 Allocation，并调用源领域 Adapter 反写。

BOTP 不直接计算业务状态，只把 active/reserved allocation 交给源领域。

## 14. 单据穿透 API

建议新增：

```text
GET /botp/documents/{system}/{type}/{id}/upstream
GET /botp/documents/{system}/{type}/{id}/downstream
GET /botp/documents/{system}/{type}/{id}/graph
GET /botp/relations/{relationId}/entries
```

Graph：

- BFS
- `depth <= 10`
- `node <= 500`
- 维护 visited DocumentKey 防止环路

## 15. BOTP 与 Reconciliation 的边界

```text
BOTP Relation
= 单据存在业务关联

3-Way Matching
= 对 PO / Receipt / Invoice 的供应商、物料、数量、单价、金额、税率等进行一致性判断
```

Relationship ≠ Reconciliation。

## 16. P0-02 验收

- PO100 → Receipt60 → PARTIAL。
- 再 Receipt40 → COMPLETE。
- 再下推1 → 拒绝。
- 作废 Receipt60 → received40 / remaining60 / PARTIAL。
- 不同系统相同 documentId 不得串单。
- Relation Entry 可准确展示每一行的上下游分配量。
