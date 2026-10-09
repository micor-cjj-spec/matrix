# P0-IMP-02 Receipt / Acceptance / Inbound 实现记录

> 状态：Implemented v1（代码已落 `dev`，未在本会话环境执行 Maven）  
> 日期：2026-08-26

## 1. 实现范围

本阶段实现采购订单后的三张业务单据：

```text
PurchaseOrder
  ↓ BOTP / 行级数量关系
PurchaseReceipt
  ↓
PurchaseAcceptance
  ↓
PurchaseInbound
  ↓
PURCHASE_INBOUND_CONFIRMED Outbox
```

本阶段不实现 AP 暂估、Accounting Event、Voucher；这些属于 P0-IMP-03。

## 2. ERP 新增对象

```text
matrix_erp_purchase_receipt
matrix_erp_purchase_receipt_entry
matrix_erp_purchase_acceptance
matrix_erp_purchase_acceptance_entry
matrix_erp_purchase_inbound
matrix_erp_purchase_inbound_entry
matrix_erp_business_event_outbox
```

采购订单行新增：

```text
freceipt_reserved_quantity
```

用于采购收货草稿/BOTP 下推预占。

## 3. 收货数量控制

采购订单行可收货数量：

```text
available
= orderQuantity
- receivedQuantity
- receiptReservedQuantity
```

创建/编辑收货单时对采购订单行执行 `SELECT ... FOR UPDATE`，先预占数量；确认收货时把预占转成已收货数量。

因此并发场景：

```text
PO remaining = 40
Receipt A = 30
Receipt B = 30
```

只有一个请求能够完成 30 的预占，第二个在获得行锁后看到 remaining=10 并失败。

## 4. 收货 → 验收

采购收货行维护：

```text
finspection_reserved_quantity
finspected_quantity
```

可验收数量：

```text
receiptQuantity
- inspectedQuantity
- inspectionReservedQuantity
```

验收分录必须满足：

```text
inspectionQuantity
=
qualifiedQuantity
+ concessionQuantity
+ rejectedQuantity
```

验收结果：

```text
PASSED
PARTIAL_PASSED
REJECTED
```

只有合格数量 + 让步接收数量大于 0 才存在可入库数量。

## 5. 验收 → 入库

验收行维护：

```text
finbound_reserved_quantity
finbound_quantity
```

可入库数量：

```text
qualifiedQuantity
+ concessionQuantity
- inboundQuantity
- inboundReservedQuantity
```

入库确认时同时：

1. 将验收行预占转成正式入库数量。
2. 反写采购订单行 `finbound_quantity`。
3. 将入库单 `faccounting_status` 置为 `PENDING`。
4. 同事务写 `PURCHASE_INBOUND_CONFIRMED` 到 ERP Outbox。

## 6. Business Event

P0-IMP-02 只完成事务内事件落库：

```text
PURCHASE_RECEIPT_CONFIRMED
PURCHASE_ACCEPTANCE_CONFIRMED
PURCHASE_INBOUND_CONFIRMED
```

其中只有 `PURCHASE_INBOUND_CONFIRMED` 是下一阶段财务暂估的触发事实。

Outbox Publisher / FI Inbox / AP Estimate 在 P0-IMP-03 继续实现。

## 7. BOTP 采购适配

BOTP 新增可识别单据类型：

```text
ERP_PURCHASE_ORDER
ERP_PURCHASE_RECEIPT
ERP_PURCHASE_ACCEPTANCE
ERP_PURCHASE_INBOUND
```

新增 ERP Feign Client 和采购 DocumentAdapter。

BOTP 执行器为每个目标分录附带：

```text
_botpSourceEntryId
_botpCorrelationKey
_botpRelationQuantity
_botpRelationAmount
```

目标单创建成功后通过 `correlationKey` 回传真实 `targetEntryId`，并持久化：

```text
matrix_botp_document_relation_entry
```

这使以下关系可查询：

```text
PO Line 11 --60--> Receipt Line 21
```

而不是只能看到 PO 和 Receipt 两张单据有关联。

## 8. 部分下推

执行参数支持：

```json
{
  "entryQuantities": {
    "<sourceEntryId>": 60
  }
}
```

用于：

```text
PO 100 → Receipt 60
Receipt 60 → Acceptance 60
Acceptance accepted 50 → Inbound 30
```

最终数量合法性仍由 ERP 行锁/预占服务校验，BOTP 参数不能绕过领域约束。

## 9. BOTP 幂等恢复

如果出现：

```text
目标单创建成功
→ Relation 保存失败
```

同一 `requestId` 对 FAILED execution 重试时复用原 `executionId`，并使用相同 target idempotency key 查询目标单；ERP 根据目标单保存的源分录 ID 恢复：

```text
correlationKey → targetEntryId
```

随后继续保存分录关系，避免重复创建目标单。

## 10. 关系完整身份

目标状态事件不再只使用：

```text
tenantId + targetDocumentId
```

而是：

```text
tenantId
+ targetSystemCode
+ targetDocumentType
+ targetDocumentId
```

防止不同系统/单据类型出现相同 ID 时误失效其他关系。

同时提供：

```text
GET /api/botp/relations/{relationId}
GET /api/botp/relations/{relationId}/entries
```

## 11. 数据库迁移

ERP：

```text
deliverables/erp/002-purchase-fulfillment/schema.sql
```

BOTP：

```text
deliverables/botp/002-p2p-fulfillment/schema.sql
```

BOTP migration 增加 Relation 和 RelationEntry 数据库唯一约束，用 DB 兜底多实例下的关系幂等。

## 12. 验收场景

应至少验证：

1. PO 100，下推 Receipt 60，PO reserved=60。
2. Receipt 确认后 PO received=60、reserved=0、receiptStatus=PARTIAL。
3. 再下推 Receipt 40 并确认后 PO received=100、receiptStatus=COMPLETE。
4. PO 剩余 40 时并发两个 30，第二个失败。
5. Receipt 60 可部分验收；验收数量必须等于合格+让步+不合格。
6. 全部不合格 Acceptance 不能生成 Inbound。
7. Acceptance 可入库 50，允许两次入库 30+20，不允许累计 51。
8. Inbound 确认后 Outbox 存在且 accountingStatus=PENDING。
9. BOTP RelationEntry 能查到 sourceEntryId、targetEntryId、quantity。
10. 不同 documentType 使用同一 documentId 时，TargetStatusEvent 不得串行失效。

## 13. 当前限制

- 本会话无法 clone 私有仓库到本地容器，因此没有实际执行 Maven test/build；测试代码与验收用例已补，但不能宣称 CI 已通过。
- ERP Business Event 目前只落 Outbox，尚未加入 RabbitMQ Dispatcher。
- BOTP 采购规则仍通过现有 Rule API 配置，不把业务映射硬编码进 Java。
- BOTP 当前采购 Adapter 的租户读取仍沿用 `botp.default-tenant`；后续应把 tenant 作为完整 DocumentKey 上下文继续收紧。
