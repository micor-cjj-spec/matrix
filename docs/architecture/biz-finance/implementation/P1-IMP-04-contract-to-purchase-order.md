# P1-IMP-04 Contract → PurchaseOrder 实现记录

> 状态：Implemented v1  
> 日期：2026-08-27  
> 目标分支：dev

## 1. 目标

将 P1 采购前置链与 P0 已有 PurchaseOrder 正式衔接：

```text
PurchaseRequest
→ RFQ / SupplierQuote
→ SourcingAward
→ PurchaseContract
→ PurchaseOrder
```

P1-IMP-04 不重新定义采购订单，而是在现有 P0 PurchaseOrder 之上增加正式合同来源、数量反写和来源追溯。

## 2. 专用转换入口

新增：

```text
POST /procurement/purchase-orders/from-contract
```

合同来源订单不允许继续通过通用创建接口手工提交 `contractId`。

正式规则：

```text
Manual PO
→ POST /procurement/purchase-orders

Contract-sourced PO
→ POST /procurement/purchase-orders/from-contract
```

这样供应商、币种、付款条件、物料、价格、税率不再由前端重复输入为权威事实。

## 3. 合同有效性

仅允许：

```text
PurchaseContract.status = EFFECTIVE
PurchaseContract.approvalStatus = APPROVED
```

的合同生成采购订单。

采购订单日期还必须落在合同有效期内：

```text
startDate <= orderDate <= endDate
```

为空的边界不限制。

## 4. PO 行级来源追溯

在 `matrix_erp_purchase_order_entry` 新增：

```text
fcontract_entry_id
fsourcing_award_entry_id
frfq_entry_id
fpurchase_request_id
fpurchase_request_entry_id
```

因此每一条采购订单分录都能直接追溯：

```text
PurchaseRequestEntry
→ RFQEntry
→ SourcingAwardEntry
→ PurchaseContractEntry
→ PurchaseOrderEntry
```

不依赖物料编码、金额或行号猜测来源。

数据库迁移：

```text
deliverables/erp/007-contract-to-purchase-order/schema.sql
```

## 5. 价格与业务快照继承

合同来源 PO 自动继承：

```text
Supplier
Currency
PaymentTerm
Material
Unit
UnitPrice
TaxRate
Project
CostCenter
PlannedDeliveryDate
```

其中价格与税率来自 PurchaseContractEntry，不允许在转换请求中覆盖。

金额由 ERP 后端计算：

```text
netAmount   = quantity × contractUnitPrice
taxAmount   = netAmount × taxRate
grossAmount = netAmount + taxAmount
```

## 6. 数量占用

创建草稿 PO 时立即占用上游数量，而不是等 PO 审核后再占用。

原因：

如果两个并发请求都在审核前读取同一合同余额，而不先占用，就可能同时生成超量草稿订单。

转换事务中同时锁定：

```text
PurchaseContract
PurchaseContractEntry
PurchaseRequestEntry
```

并校验：

```text
orderQuantity
<= contractEntry.quantity - contractEntry.orderedQuantity
```

以及：

```text
orderQuantity
<= purchaseRequestEntry.quantity - purchaseRequestEntry.orderedQuantity
```

成功后同事务回写：

```text
PurchaseContractEntry.orderedQuantity += orderQuantity
PurchaseRequestEntry.orderedQuantity += orderQuantity
```

## 7. 执行状态

采购合同执行状态：

```text
NONE
ORDERING
COMPLETE
```

规则：

```text
无已下单数量      → NONE
部分已下单        → ORDERING
全部合同量已下单  → COMPLETE
```

采购申请执行状态根据 sourced / ordered 数量重新计算：

```text
SOURCING
CONTRACTING
ORDERING
COMPLETE
```

典型状态：

```text
已完全寻源但未下单 → CONTRACTING
部分下单           → ORDERING
全部需求已下单     → COMPLETE
```

## 8. 删除 / 取消释放

合同来源 PO 的数量在 Draft 创建时已经占用，因此必须支持反向释放。

### 草稿删除

删除草稿 PO 时：

```text
ContractEntry.orderedQuantity -= PO quantity
PurchaseRequestEntry.orderedQuantity -= PO quantity
```

然后重新计算 Contract / PurchaseRequest executionStatus。

### 有效订单取消

现有 P0 规则仍保持：

只有：

```text
status = EFFECTIVE
approvalStatus = AUDITED
receiptStatus = NONE
```

才允许直接取消。

合同来源 PO 在满足上述条件并取消时，同样释放合同与采购申请的 orderedQuantity。

一旦发生收货，不允许直接取消并释放，必须走后续退货/关闭流程。

## 9. 通用编辑保护

合同来源 PO 不允许通过通用：

```text
PUT /procurement/purchase-orders/{fid}
```

修改。

原因：

合同来源订单的：

```text
supplier
price
tax
material
source quantities
```

属于上游合同权威事实。

v1 如需修改合同来源草稿订单，采用：

```text
删除草稿
→ 释放上游数量
→ 重新 from-contract 生成
```

避免通用编辑导致来源数量和价格漂移。

## 10. BOTP 来源透传

现有 `ERP_PURCHASE_ORDER` BOTP Document 的 entry 增加：

```text
contractEntryId
sourcingAwardEntryId
rfqEntryId
purchaseRequestId
purchaseRequestEntryId
```

因此后续 BOTP 转换链可以直接使用真实业务来源。

P1-IMP-04 只暴露来源事实，不在本阶段提前实现完整 BOTP Relation 持久化。

完整 Procurement BOTP Completion 仍属于 P1-IMP-07。

## 11. Tests

新增：

```text
PurchaseOrderContractConversionServiceTest
```

覆盖：

1. EFFECTIVE + APPROVED 合同生成 Draft PO。
2. Supplier / Currency / Price / Tax 从合同继承。
3. PO 行保留完整上游来源。
4. ContractEntry.orderedQuantity 回写。
5. PurchaseRequestEntry.orderedQuantity 回写。
6. 全量下单后 Contract → COMPLETE。
7. 全量下单后 PurchaseRequest → COMPLETE。
8. 超合同剩余量拒绝。
9. 超出合同有效期拒绝。
10. 删除/取消释放后状态恢复。

原 `PurchaseOrderServiceTest` 同步回归手工 PO 能力。

## 12. PR / CI

实现 PR：

```text
PR #86
feat(procurement): implement P1 contract to purchase order
```

合并提交：

```text
9175a7559c17d2feee2ca0243c0a506a7dc3365e
```

PR 门禁：

```text
Repository Hygiene CI  success
Biz Finance P0 CI      success
```

开发过程中发现并修复一次工具写入导致的 `ProcurementBotpController` 文件截断，最终 PR 差异恢复为预期规模并通过完整 CI。

## 13. 当前边界

P1-IMP-04 已完成：

```text
PurchaseRequest
→ Sourcing
→ PurchaseContract
→ PurchaseOrder
```

代码级业务闭环。

当前未完成：

- DeliveryPlan
- Supplier Collaboration
- Purchase Return
- Claim / Deduction
- Procurement BOTP Relation 全量持久化
- 合同变更后订单重算
- Supplier Portal

## 14. 下一阶段

```text
P1-IMP-05 DeliveryPlan / Supplier Collaboration
```

下一步补齐采购订单生效后的供应商协同和交付计划事实，为收货前的业务执行段增加正式计划层。
