# P1-IMP-07 Procurement BOTP Completion 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

P1-IMP-07 不新增采购业务对象，而是把 P0 / P1 已完成的采购事实正式收口到 BOTP：

```text
PurchaseRequest
→ RFQ
→ SourcingAward
→ PurchaseContract
→ PurchaseOrder
→ DeliveryPlan
→ PurchaseReceipt
→ PurchaseAcceptance
→ PurchaseInbound
→ PurchaseReturn
→ SupplierClaim
→ PurchaseDeduction
```

BOTP 在本阶段承担：

- Procurement Document Adapter
- Document Relation
- Relation Entry
- Full-key upstream/downstream trace
- Document Graph
- Deterministic document conversion
- Target idempotency
- Historical/domain-created lineage synchronization

BOTP 仍不负责：

- 询价定标决策
- 供应商选择策略
- 质量判定
- 索赔金额协商
- 会计规则和凭证

## 2. Procurement Document Surface

ERP internal BOTP document surface 扩展到：

```text
ERP_PURCHASE_REQUEST
ERP_PROCUREMENT_RFQ
ERP_SOURCING_AWARD
ERP_PURCHASE_CONTRACT
ERP_PURCHASE_ORDER
ERP_PURCHASE_DELIVERY_PLAN
ERP_PURCHASE_RECEIPT
ERP_PURCHASE_ACCEPTANCE
ERP_PURCHASE_INBOUND
ERP_PURCHASE_RETURN
ERP_SUPPLIER_CLAIM
ERP_PURCHASE_DEDUCTION
```

所有 entry 返回：

- entryId
- 真实业务来源 ID / Entry ID
- quantity / amount（适用时）
- availableQuantity（适用时）
- Material / Project / CostCenter 等必要快照

这些字段用于 BOTP Relation / Entry Relation，不通过行号猜测来源。

## 3. Full DocumentKey

新增：

```java
DocumentKey(
    tenantId,
    systemCode,
    documentType,
    documentId
)
```

上下游查询和 Graph traversal 使用完整键。

不允许再以裸 documentId 作为图遍历唯一身份。

例如：

```text
ERP_PURCHASE_REQUEST / 100
ERP_PURCHASE_CONTRACT / 100
```

属于两个不同节点。

## 4. Graph API

新增：

```text
GET /botp/relations/documents/{system}/{type}/{id}/upstream
GET /botp/relations/documents/{system}/{type}/{id}/downstream
GET /botp/relations/documents/{system}/{type}/{id}/graph
```

均要求 tenantId。

Graph：

```text
BFS
depth <= 10
node <= 500
visited = full DocumentKey
```

返回：

- nodes
- edges
- relation status
- relation entry quantity total
- relation entry amount total
- truncated

## 5. Procurement Relation Sync

新增：

```text
ProcurementRelationSyncService
```

以及：

```text
POST /botp/relations/procurement/documents/{type}/{id}/sync
```

作用：

将业务域已经存在的正式来源事实同步进：

```text
matrix_botp_document_relation
matrix_botp_document_relation_entry
```

这解决了一个重要边界：

并不是所有采购单据都由 BOTP 自动生成。

例如：

- SourcingAward 是人工/业务决策结果。
- SupplierClaim 金额可能经过业务协商。
- 历史数据在 BOTP Relation 正式启用前已经存在。

因此不能为了建立 BOTP Relation 而重新创建业务单据。

正式方式：

```text
existing business lineage
→ ProcurementRelationSyncService
→ Header Relation
→ Entry Relation
```

## 6. Lineage Mapping

v1 同步支持：

```text
PurchaseRequestEntry
→ RFQEntry

RFQEntry
→ SourcingAwardEntry

SourcingAwardEntry
→ PurchaseContractEntry

PurchaseContractEntry
→ PurchaseOrderEntry

PurchaseOrderEntry
→ DeliveryPlanEntry

PurchaseOrderEntry
→ PurchaseReceiptEntry

PurchaseReceiptEntry
→ PurchaseAcceptanceEntry

PurchaseAcceptanceEntry
→ PurchaseInboundEntry

PurchaseInboundEntry
→ PurchaseReturnEntry

PurchaseReturnEntry / PurchaseOrderEntry
→ SupplierClaimEntry

SupplierClaimEntry
→ PurchaseDeductionEntry
```

一张目标单如果来自多个源单，按 source document 分组创建多个 Header Relation。

## 7. Executable Procurement Rules

P1-IMP-07 将确定性转换注册为 BOTP Built-in Rule：

```text
PURCHASE_CONTRACT_TO_ORDER

PURCHASE_ORDER_TO_RECEIPT

PURCHASE_RECEIPT_TO_ACCEPTANCE

PURCHASE_ACCEPTANCE_TO_INBOUND
```

其中：

### Contract → PO

使用已有：

```text
PurchaseOrderContractConversionService
```

BOTP 只负责：

- Source load
- Rule mapping
- Target idempotency
- Relation / RelationEntry
- Trace

供应商、价格、税率、合同剩余数量、采购申请剩余数量等仍由 ERP 领域服务校验。

### PO → Receipt → Acceptance → Inbound

继续使用原 P0 领域服务创建目标单。

原领域服务负责：

- 行锁
- 数量预占
- 超量校验
- 状态推进

BOTP 不复制这些领域规则。

## 8. PurchaseOrder Target Idempotency

为 Contract → PO 增加：

```text
PurchaseOrder.fbotpIdempotencyKey
PurchaseOrder.fsourceExecutionId
```

数据库迁移：

```text
deliverables/erp/010-procurement-botp/schema.sql
```

唯一约束：

```text
tenantId + botpIdempotencyKey
```

故障窗口：

```text
Create PO success
→ BOTP process crashes before Relation save
→ retry
```

重试时通过相同 idempotencyKey 找回原 PO，不重新创建目标单，因此不会：

- 重复占用 ContractEntry.orderedQuantity
- 重复占用 PurchaseRequestEntry.orderedQuantity
- 生成重复采购订单

## 9. Tenant Correctness

修正已有 ERP Adapter 的 tenant 加载方式。

旧行为：

```text
ExecutionRequest.tenantId
×
Adapter configured default tenant
```

可能不一致。

现在：

```text
ExecutionRequest.tenantId
→ BotpDocumentAdapter.load(documentRef, tenantId)
→ ERP document API
```

default tenant 仅作为没有显式 tenant 时的兼容 fallback。

## 10. Built-in Rule Initialization

`BotpBuiltInRuleInitializer` 不再只在 MySQL persistence mode 下运行。

现在：

```text
memory
mysql
```

都会进行 built-in rule 初始化。

初始化逻辑本身通过：

```text
findPublishedByCode
```

保持幂等，不重复创建版本。

## 11. Tests

新增：

```text
BotpDocumentGraphServiceTest
ProcurementRelationSyncServiceTest
BotpBuiltInRuleInitializerTest
```

主要覆盖：

1. 完整 DocumentKey 防止裸 ID 串单。
2. BFS Graph 上下游穿透。
3. Graph depth bound。
4. Contract → Award lineage 的 Header / Entry Relation。
5. Relation Sync 重复执行幂等。
6. SupplierClaim 优先追溯 PurchaseReturn。
7. memory mode 也初始化采购 Built-in Rule。
8. Built-in Rule 初始化重复执行不产生新版本。

另外 Biz Finance P0 CI 覆盖 ERP DTO / Entity / Contract → PO 回归。

## 12. PR / CI

实现 PR：

```text
PR #90
feat(botp): complete P1 procurement document relations
```

PR head：

```text
9f1967b026009b01b106589a78238370a79e1626
```

合并提交：

```text
3245b918a0f2dafefb693518f4b91328e22dd0e2
```

PR 门禁：

```text
Repository Hygiene CI  33059165873 success
BOTP CI                33059165717 success
Biz Finance P0 CI      33059165844 success
```

## 13. 当前边界

P1-IMP-07 完成的是采购 BOTP v1。

仍未包含：

- 自动批量扫描所有历史采购单据并 backfill relation
- Supplier Portal
- RFQ → Award 自动定标
- Claim 自动协商
- 通用多源合并目标单
- 生产环境 runtime smoke
- 全链压测 / 故障注入

其中 RFQ → Award 不做自动转换是有意边界：

```text
comparison
≠
award decision
```

BOTP 不替代人工/策略定标。

## 14. P1 收口

P1 采购业务覆盖至此完成：

```text
PurchaseRequest
→ Sourcing
→ Contract
→ PurchaseOrder
→ Delivery Collaboration
→ Receipt / Acceptance / Inbound
→ Return / Claim / Deduction
→ AP / Accounting
```

并具备：

```text
Business Document
+ Business Event
+ BOTP Relation
+ Entry Relation
+ Graph Trace
+ Idempotency
+ Accounting Boundary
```

下一阶段不应继续无边界扩张 Procurement，而应重新根据业务设计文档确定下一条业务域主链。
